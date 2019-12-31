package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.math._
import cats.data.State
import cats.implicits._
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  type Triage = Map[Score, List[Waiting]]

  private val waitingQueue = mutable.Queue.empty[Waiting]

  /*private val highestScore = Ref(Score(0))

  private val waitingPlayers: TMap[Score, List[Waiting]] = TMap.empty[Score, List[Waiting]]

  val matching: CancelableFuture[Nothing] =
    Task(doMatch()).executeAsync.loopForever.runToFuture*/


  val receive: Input => Unit = {
    case w: Waiting =>
      waitingQueue enqueue w

      // TODO
      // println(waitingPlayers.mkString("\n"))
  }

  val matching: CancelableFuture[(Triage, List[Match])] =
    Task {
      println("===> Begin matching")
      doMatch().run(Map.empty).value
    }.executeAsync.runToFuture
  //Task(doMatch().run(Map.empty).value).executeAsync.runToFuture


  def waitingQueueSnapshot: List[Waiting] =
    waitingQueue.toList

  def doMatch(): State[Triage, List[Match]] =
    for {
      _ <- State.get[Triage]
      _ <- dequeueWaiting
      matches <- findMatches
      _ <- if (matching.isCompleted) State.get[Triage] else {
        TimeUnit.SECONDS.sleep(3) // TODO - Remove
        doMatch()
      }
    } yield matches

  def dequeueWaiting: State[Triage, Unit] =
    State.modify[Triage] { triage =>
      waitingQueue.dequeueAll(_ => true).foldLeft(triage) { (triage, w) =>
        triage.updatedWith(w.player.score) {
          case None => Option(List(w))
          case waiting => waiting.map(_ :+ w)
        }
      }
    }

  def findMatches: State[Triage, List[Match]] =
    State[Triage, List[Match]] { triage =>
      val (nextTriage, matches) = findMatches(triage)

      println(s"\n ===> Next triage: $nextTriage \n")
      println(s"\n ===> Waiting: $waitingQueue \n")

      (nextTriage, matches)
    }

  def findMatches(triage: Triage): (Triage, List[Match]) = {
    val waitings = triage.values.flatten.toList.sortWith(_.player.score > _.player.score)
    findMatches(waitings, triage)
  }

  def findMatches(waitings: List[Waiting], triage: Triage, matches: List[Match] = Nil): (Triage, List[Match]) =
    waitings match {
      case w +: rest =>
        findMatch(w, triage).fold(findMatches(rest, triage, matches)) { newMatch =>
          val stopWaiting: Player => Triage => Triage = { player =>
            _.updatedWith(player.score)(_.map(_.filterNot(_.player == player)))
          }

          val newTriage = (stopWaiting(newMatch.playerA) andThen stopWaiting(newMatch.playerB))(triage)

          findMatches(rest, newTriage, matches :+ newMatch)
        }

      case _ =>
        (triage, matches)
    }

  def findMatch(waiting: Waiting, triage: Triage): Option[Match] = {
    val waitingPlayer = waiting.player
    val waitings: List[Waiting] = triage.getOrElse(waitingPlayer.score, Nil)

    waitings.filterNot(_.player == waitingPlayer).filterNot(_.player.played.contains(waitingPlayer)).headOption match {
      case Some(w) =>
        createMatch(waitingPlayer, w.player).some

      case None =>
        if (waiting.elapsedMs() - waiting.startedMs <= config.maxWaitMs)
          None
        else
          findMatch(waitingPlayer, triage)
    }
  }

  def findMatch(player: Player, triage: Triage): Option[Match] = {
    val lowScore = max(0, player.score.value - config.maxScoreDelta).toInt
    val highScore = (player.score.value + config.maxScoreDelta).toInt

    val scoresBelow = (lowScore until player.score.value).map(Score.apply).flatMap(triage.get).flatten
    val scoresAbove = (player.score.value + 1 to highScore).map(Score.apply).flatMap(triage.get).flatten

    val waitings: Seq[Waiting] = (scoresBelow ++ scoresAbove).sortWith((w1, w2) => scoreDelta(w1) < scoreDelta(w2))

    waitings.headOption.fold(none[Match])(w => createMatch(player, w.player).some)
  }

  def scoreDelta(waiting: Waiting): Int =
    abs(config.maxScoreDelta - waiting.player.score.value).toInt

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


  /*

  def doMatch(): List[Match] = {
    import cats.data.OptionT
    import cats.data.OptionT._

    val (waiting, startingScore) = atomic { implicit txn =>
      (waitingPlayers.snapshot, highestScore())
    }

    val matches: OptionT[List, Match] = for {
      s  <- liftF((startingScore.value to 0 by -1).toList.map(Score.apply))
      ws <- fromOption[List](waiting.get(s))
      w  <- liftF(ws)
      m  <- fromOption[List](findMatch(w, w.player.score))
    } yield m

    matches.value.flatten
  }

  /**
   * Matching a player in waiting first tries for a player of equal score (as provided by matchingScore).
   * If none is available but a player has exceeded their wait time,
   * then look for players of lesser score (as provided by matchingScore.
   * @param waiting Waiting
   * @param matchingScore Score
   * @return Option[Match]
   */
  def findMatch(waiting: Waiting, matchingScore: Score): Option[Match] =
    atomic { implicit txn =>
      Option.when(waitingPlayers.get(waiting.player.score).exists(_.contains(waiting))) {
        waitingPlayers.get(matchingScore).flatMap {
          _.filterNot(_.player == waiting.player).collectFirst {
            case w: Waiting if matchAllowed(waiting.player, w.player) => w
          }
        }
      } collectFirstSome {
        case Some(w) => Option(createMatch(waiting, w))
        case None => unmatched(waiting, matchingScore)
      }
    }

  def matchAllowed(player: Player, opponent: Player): Boolean =
    !player.played.contains(opponent) && player.score.difference(opponent.score) <= Score(config.maxScoreDelta.toInt)

  def unmatched(waiting: Waiting, matchingScore: Score): Option[Match] = {
    if (waiting.elapsedMs() - waiting.startedMs > config.maxWaitMs)
      matchingScore.decrement.flatMap(findMatch(waiting, _))
    else
      None
  }*/
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}