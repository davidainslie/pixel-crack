package com.backwards.pixel

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.stm._
import cats.Eval
import cats.data.State
import cats.implicits._
import monix.eval.Task
import monix.execution.atomic.AtomicInt
import monix.execution.{CancelableFuture, Scheduler}
import monocle.macros.syntax.lens._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  type Triage = Map[Score, List[Waiting]]

  private val waiting = mutable.Queue.empty[Waiting]

  /*private val highestScore = Ref(Score(0))

  private val waitingPlayers: TMap[Score, List[Waiting]] = TMap.empty[Score, List[Waiting]]

  val matching: CancelableFuture[Nothing] =
    Task(doMatch()).executeAsync.loopForever.runToFuture*/

  val receive: Input => Unit = {
    case w: Waiting =>
      waiting enqueue w

      // TODO
      // println(waitingPlayers.mkString("\n"))
  }

  def waitingSnapshot: List[Waiting] =
    waiting.toList

  val temp = new AtomicInteger(0)

  def doMatch(): State[Triage, String] =
    for {
      _ <- State.get[Triage]
      blah <- nextTriage
      zz <- if (temp.get > 3) State.get[Triage] else {
        TimeUnit.SECONDS.sleep(3)
        doMatch()
      }
      //xx <- matchGames
    } yield blah

  def nextTriage: State[Triage, String] =
    State[Triage, String] { triage =>
      val nextTriage: Triage = waiting.dequeueAll(_ => true).foldLeft(triage) { (triage, w) =>
        triage.updatedWith(w.player.score) {
          case None => Option(List(w))
          case waiting => waiting.map(_ :+ w)
        }
      }

      println(s"\n ===> Next triage: $nextTriage \n")
      println(s"\n ===> Waiting: $waiting \n")
      temp.incrementAndGet()

      (nextTriage, "boo")
    }

  //def matchGames : State[Matchings, String] = ???

  //val v: (GameState, String) = doMatch.run(GameState(Map.empty[Score, Waiting])).value

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

  /**
   * Create a Match for two players where the player with lowest ID will be given first in Match(player1, player2)
   * @param waiting Waiting
   * @param matchedWaiting Waiting
   * @return Match
   */
  def createMatch(waiting: Waiting, matchedWaiting: Waiting): Match = {
    val newMatch = if (waiting.player.id <= matchedWaiting.player.id)
      Match(waiting.player, matchedWaiting.player)
    else
      Match(matchedWaiting.player, waiting.player)

    atomic { implicit txn =>
      val stopWaiting: Waiting => Unit = { w =>
        waitingPlayers.updateWith(w.player.score)(_.map(_.filterNot(_ == w)))
      }

      stopWaiting(waiting)
      stopWaiting(matchedWaiting)
    }

    println(newMatch)
    newMatch
  }

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