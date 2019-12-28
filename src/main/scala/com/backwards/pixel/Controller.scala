package com.backwards.pixel

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.collection.concurrent.TrieMap
import scala.collection.Map
import scala.collection.Map
import cats.Id
import cats.effect.IO
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import scala.concurrent.duration._
import scala.concurrent.stm._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  private val highestScore = new AtomicReference[Score](Score(0))

  // TODO - Should this be a TMap ???
  private val waitingPlayers: TrieMap[Score, Set[Waiting]] = new TrieMap[Score, Set[Waiting]]

  val matching: CancelableFuture[Nothing] = Task {
    println("Hi")
    doMatch()
  }.executeAsync.doOnCancel(Task(println("Matching ended"))).loopForever.runToFuture


  // Over to you ...
  /*def receive(in: Input): Unit = {
    // println(s"Received $in")

    println(s"===> Controller: received = $in")
  }*/

  val receive: Input => Unit = {
    case w: Waiting =>
      waitingPlayers.updateWith(w.player.score) {
        case None => Option(Set(w))
        case waiting => waiting.map(_ + w)
      }

      println(waitingPlayers.mkString("\n"))
  }

  def waitingPlayersSnapshot: Map[Score, Set[Waiting]] =
    waitingPlayers.readOnlySnapshot()

  def doMatch(): Option[Match] = {

  }
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}