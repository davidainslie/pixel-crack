package com.backwards.pixel

import scala.collection.Map
import scala.concurrent.stm._
import cats.data.OptionT
import cats.implicits._
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import monocle.Prism
import monocle.macros.syntax.lens._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  private val highestScore = Ref(Score(0))

  private val waitingPlayers: TMap[Score, List[Waiting]] = TMap.empty[Score, List[Waiting]]

  val matching: CancelableFuture[Nothing] =
    Task(doMatch()).executeAsync.loopForever.runToFuture

  val receive: Input => Unit = {
    case waiting: Waiting =>
      atomic { implicit txn =>
        waitingPlayers.updateWith(waiting.player.score) {
          case None => Option(List(waiting))
          case w => w.map(_ :+ waiting)
        }

        Option.when(waiting.player.score > highestScore()) {
          highestScore() = waiting.player.score
        }

        println(waitingPlayers.mkString("\n"))
      }
  }

  def waitingPlayersSnapshot: Map[Score, List[Waiting]] =
    atomic { implicit txn =>
      waitingPlayers.snapshot
    }

  def doMatch(): Option[Match] = {
    val (waiting, startingScore) = atomic { implicit txn =>
      (waitingPlayers.snapshot, highestScore())
    }

    val v = for {
      s <- OptionT.liftF((startingScore.value to 0 by -1).toList.map(Score.apply))
      ws <- OptionT.fromOption[List](waiting.get(s))
      w <- OptionT.liftF(ws)
    } yield {
      println(w)
      findMatch(w, w.player.score)
      ws
    }

    println(v.value)

    ???
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
    atomic(implicit txn =>
      waitingPlayers.get(matchingScore).map(_.filterNot(_.player == waiting.player)).flatMap(_.collectFirst {
        case w: Waiting if matchAllowed(waiting.player, w.player) => w
      })
    ).fold(unmatched(waiting, matchingScore))(createMatch(waiting) andThen Option.apply)

  def matchAllowed(player: Player, opponent: Player): Boolean =
    !player.played.contains(opponent) && player.score.difference(opponent.score) <= Score(config.maxScoreDelta.toInt)

  /**
   * Create a Match for two players where the player with lowest ID will be given first in Match(player1, player2)
   * @param waiting Waiting
   * @return Match
   */
  def createMatch(waiting: Waiting): Waiting => Match = { matchedWaiting =>
    if (waiting.player.id <= matchedWaiting.player.id)
      Match(waiting.player, matchedWaiting.player)
    else
      Match(matchedWaiting.player, waiting.player)
  }

  def unmatched(waiting: Waiting, matchingScore: Score): Option[Match] = {
    if (waiting.elapsedMs() - waiting.startedMs > config.maxWaitMs)
      matchingScore.decrement.flatMap(findMatch(waiting, _))
    else
      None
  }
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}