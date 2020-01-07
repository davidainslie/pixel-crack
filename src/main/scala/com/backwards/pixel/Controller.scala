package com.backwards.pixel

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.concurrent.duration._
import scala.math._
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Fiber, IO, Timer}
import cats.implicits._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit Concurrent: Concurrent[IO], Timer: Timer[IO]) {
  type Triage = Map[Score, List[Waiting]]
  type Matching = AtomicBoolean

  val receive: Input => Unit = {
    case w: Waiting =>
      waitingQueue.update(_ enqueue w).unsafeRunSync

    case g: GameCompleted =>
      scribe debug g.show

      def issueWaitingEvent(player: Player): Unit =
        if (!player.expired) receive(Waiting(player))

      List(g.winner, g.loser).foreach(issueWaitingEvent)
  }

  private val waitingQueue: Ref[IO, mutable.Queue[Waiting]] =
    Ref.unsafe[IO, mutable.Queue[Waiting]](mutable.Queue.empty[Waiting])

  private val matching: (Fiber[IO, (Triage, List[Match])], Matching) =
    startMatching {
      IO(scribe info "Begin matching...") *> doMatch(Ref.unsafe[IO, Triage](Map.empty))
    }

  def startMatching(matching: IO[(Triage, List[Match])]): (Fiber[IO, (Triage, List[Match])], Matching) =
    Concurrent.start(matching).unsafeRunSync -> new AtomicBoolean(true)

  // TODO - Should have used something like a Resource[_, _] or at least a Bracket to not have to remember to call this
  def shutdown(): Unit =
    matching.bimap(_.cancel, _ set false)

  def waitingQueueSnapshot: List[Waiting] =
    waitingQueue.get.map(_.toList).unsafeRunSync

  def doMatch(triage: Ref[IO, Triage]): IO[(Triage, List[Match])] =
    for {
      waitings <- waitingQueue modify { waitings =>
        val ws = waitings.dequeueAll(_ => true)
        (waitings, ws)
      }
      _ <- triage update add(waitings)
      matches <- triage modify findMatches
      triageAndMatches <- if (matching._2.get) IO.sleep(1 second) *> doMatch(triage) else triage.get.map(_ -> matches)
    } yield triageAndMatches

  def add(waitings: Seq[Waiting])(triage: Triage): Triage =
    waitings.foldLeft(triage) { (triage, w) =>
      triage.updatedWith(w.player.score) {
        case None => Option(List(w))
        case waiting => waiting.map(_ :+ w)
      }
    }

  /**
   * Find matches in descending order of player's score i.e. top ranking players are prioritised.
   * @param triage Triage
   * @return (Triage, List[Match])
   */
  def findMatches(triage: Triage): (Triage, List[Match]) = {
    def findMatches(triage: Triage, matches: List[Match]): List[Waiting] => (Triage, List[Match]) = {
      case w +: restOfWaiting =>
        findMatch(w, triage).fold(findMatches(triage, matches)(restOfWaiting)) { newMatch =>
          val stopWaiting: Player => Triage => Triage = { player =>
            _.updatedWith(player.score)(_.map(_.filterNot(_.player == player)))
          }

          val filterMatch: Player => List[Waiting] => List[Waiting] = { player =>
            _.filterNot(_.player == player)
          }

          val nextTriage = (stopWaiting(newMatch.playerA) andThen stopWaiting(newMatch.playerB))(triage)
          val nextRestOfWaiting = (filterMatch(newMatch.playerA) andThen filterMatch(newMatch.playerB))(restOfWaiting)

          issueMatchEvent(newMatch)
          findMatches(nextTriage, matches :+ newMatch)(nextRestOfWaiting)
        }

      case _ =>
        (triage, matches)
    }

    val waitings = triage.values.flatten.toList.sortWith(_.player.score > _.player.score)
    findMatches(triage, Nil)(waitings)
  }

  def findMatch(waiting: Waiting, triage: Triage): Option[Match] = {
    val player = waiting.player

    def findMatch(waitings: Seq[Waiting]): Option[Match] =
      waitings.headOption.map(_.player).map(createMatch(player, _))

    findMatch(waitingsOfSameScore(player, triage)) orElse {
      Option.when(overdue(waiting))(findMatch(waitingsWithinScoreDelta(player, triage))).flatten
    }
  }

  /**
   * All waiting players with same score as given player
   * @param player Player
   * @param triage Triage
   * @return Seq[Waiting]
   */
  def waitingsOfSameScore(player: Player, triage: Triage): Seq[Waiting] =
    canPlay(triage.getOrElse(player.score, Nil))(player)

  /**
   * All waiting players within the score delta (maximum configured) of given player,
   * EXCLUDING players of equal score @see waitingsOfSameScore
   * @param player Player
   * @param triage Triage
   * @return Seq[Waiting]
   */
  def waitingsWithinScoreDelta(player: Player, triage: Triage): Seq[Waiting] = {


    val lowScore = BigDecimal(0).max(player.score.minus(Score(config.maxScoreDelta)).value)
    val highScore = (player.score.value.toDouble + config.maxScoreDelta)

    scala.collection.immutable.Range.BigDecimal(lowScore, player.score, step)

    val scoresBelow = (lowScore until player.score).flatMap(triage.get).flatten
    val scoresAbove = (player.score + 1 to highScore).flatMap(triage.get).flatten

    val waitings = (scoresBelow ++ scoresAbove).sortWith { (w1, w2) =>
      scoreDelta(player, w1.player) < scoreDelta(player, w2.player)
    }

    canPlay(waitings)(player)
  }

  def canPlay(waitings: Seq[Waiting])(player: Player): Seq[Waiting] = {
    val isPlayer: Waiting => Boolean =
      _.player == player

    val hasPlayed: Waiting => Boolean =
      w => player.played.contains(w.player.id) || w.player.played.contains(player.id)

    waitings.filterNot(isPlayer).filterNot(hasPlayed)
  }

  def scoreDelta(p1: Player, p2: Player): Int =
    abs(p1.score - p2.score)

  def overdue(waiting: Waiting): Boolean =
    waiting.elapsedMs() - waiting.startedMs > config.maxWaitMs

  /**
   * Create a Match for two players where the player with lowest ID will be given first in Match(player1, player2)
   * @param player1 Player
   * @param player2 Player
   * @return Match
   */
  def createMatch(player1: Player, player2: Player): Match = {
    if (player1.id <= player2.id)
      Match(player1, player2)
    else
      Match(player2, player1)
  }

  private def issueMatchEvent(m: Match): Unit = out(m)
}

object Controller {
  def apply(config: Config)(implicit concurrent: Concurrent[IO], Timer: Timer[IO]): (Output => Unit) => Controller =
    new Controller(config, _)
}