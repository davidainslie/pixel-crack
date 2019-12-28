package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import scala.collection.Map
import cats.Id
import cats.effect.IO
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import scala.concurrent.duration._

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit)(implicit scheduler: Scheduler) {
  // TODO - Should this be a TMap ???
  private val waiting: TrieMap[Score, Waiting] = new TrieMap[Score, Waiting]

  val matching: CancelableFuture[Nothing] = Task {
    println("Hi")
    TimeUnit.SECONDS.sleep(3)
  }.executeAsync.doOnCancel(Task(println("Matching ended"))).loopForever.runToFuture


  // Over to you ...
  /*def receive(in: Input): Unit = {
    // println(s"Received $in")

    println(s"===> Controller: received = $in")
  }*/

  val receive: Input => Unit = {
    case w: Waiting =>
      waiting.put(w.player.score, w)

      /*waiting.values.foreach { player =>
        println(s"===> Player expired? ${player.expired}")
      }*/

      //println(q.dequeue.compile.last.unsafeRunSync())

      println(waiting.mkString("\n"))
  }

  def playersInWaiting: Map[Score, Waiting] =
    waiting.readOnlySnapshot()
}

object Controller {
  def apply(config: Config)(implicit scheduler: Scheduler): (Output => Unit) => Controller =
    new Controller(config, _)
}