package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.Promise
import scala.math._
import cats.data.State
import cats.implicits._
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import scala.concurrent.stm._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  type Score = Int // TODO - Originally had a Score ADT but reverted to simply Int, can't decide if this was wise.
  type Triage = Map[Score, List[Waiting]]

  private val waitingQueue = Ref(mutable.Queue.empty[Waiting])

  val receive: Input => Unit = {
    case w: Waiting =>
      waitingQueue.single.transform(_ enqueue w)

      // TODO
      // println(waitingPlayers.mkString("\n"))
  }

  private val matching: CancelableFuture[(Triage, List[Match])] = {
    def start: Task[(Triage, List[Match])] = Task {
      println("===> Begin matching")
      doMatch().run(Map.empty).value
    }

    def stop: Task[Unit] = Task(println("===> Matching ceased"))

    start.doOnCancel(stop).executeAsync.runToFuture
  }

  private val isShutdown: Promise[Boolean] = Promise[Boolean]()

  def shutdown(): Unit = {
    matching.cancel()
    isShutdown.success(true)
  }

  def waitingQueueSnapshot: List[Waiting] =
    waitingQueue.single.get.toList

  def doMatch(): State[Triage, List[Match]] =
    for {
      _ <- State.modify(dequeueWaiting)
      matches <- State(findMatches)
      _ <- if (isShutdown.isCompleted) State.get[Triage] else {
        TimeUnit.SECONDS.sleep(3) // TODO - Remove
        doMatch()
      }
    } yield matches

  def dequeueWaiting(triage: Triage): Triage = {
    println(s"===> dequeue")

    val waitings: Seq[Waiting] = atomic { implicit txn =>
      waitingQueue().dequeueAll(_ => true)
    }

    waitings.foldLeft(triage) { (triage, w) =>
      triage.updatedWith(w.player.score) {
        case None => Option(List(w))
        case waiting => waiting.map(_ :+ w)
      }
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
    filter(triage.getOrElse(player.score, Nil))(player)

  /**
   * All waiting players within the score delta (maximum configured) of given player,
   * EXCLUDING players of equal score @see waitingsOfSameScore
   * @param player Player
   * @param triage Triage
   * @return Seq[Waiting]
   */
  def waitingsWithinScoreDelta(player: Player, triage: Triage): Seq[Waiting] = {
    val lowScore = max(0, player.score - config.maxScoreDelta).toInt
    val highScore = (player.score + config.maxScoreDelta).toInt

    val scoresBelow = (lowScore until player.score).flatMap(triage.get).flatten
    val scoresAbove = (player.score + 1 to highScore).flatMap(triage.get).flatten

    val waitings = (scoresBelow ++ scoresAbove).sortWith { (w1, w2) =>
      scoreDelta(player, w1.player) < scoreDelta(player, w2.player)
    }

    filter(waitings)(player)
  }

  def filter(waitings: Seq[Waiting])(player: Player): Seq[Waiting] = {
    val isPlayer: Waiting => Boolean = _.player == player

    val hasPlayed: Waiting => Boolean = _.player.played.contains(player)

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
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}