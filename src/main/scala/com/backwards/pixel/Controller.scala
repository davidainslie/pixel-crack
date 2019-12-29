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
      doMatch(w)
      ws
    }

    println(v.value)

    ???
  }

  /**
   * Matching a player in waiting first tries for a player of equal score.
   * If none is available but a player has exceeded their wait time,
   * then look for players of lesser score.
   * @param waiting Waiting
   * @return Option[Match
   */
  def doMatch(waiting: Waiting): Option[Match] = {
    val player = waiting.player

    atomic { implicit txn =>
      waitingPlayers.get(waiting.player.score).map(_.filterNot(_ == waiting)).flatMap(_.collectFirst {
        case w @ Waiting(potentialPlayer, _) if !player.played.contains(potentialPlayer) => w
      })
    } map { matchedWaiting =>
      if (player.id <= matchedWaiting.player.id)
        Match(player, matchedWaiting.player)
      else
        Match(matchedWaiting.player, player)
    }
  }
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}