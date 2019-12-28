package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import cats.Id
import cats.effect.IO
import monix.eval.Task
import monix.execution.Scheduler

/** We presume this `Controller` is managed somehow such that it is instantiated,
 * receives input somehow, and can appropriately process the output. The config
 * is liable to change during operation. There will be exactly one instance
 * of this controller created per-runtime (but `receive` could be called by
 * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit) {
  // TODO - Should this be a TMap ???
  val waiting: TrieMap[Score, Waiting] = new TrieMap[Score, Waiting]

  import scala.concurrent.duration._
  import scala.concurrent.TimeoutException

  // In order to evaluate tasks, we'll need a Scheduler
  import monix.execution.Scheduler.Implicits.global

  val t = Task {
    println("Hi")
    TimeUnit.SECONDS.sleep(3)
  }//.loopForever.executeWithOptions(_.enableAutoCancelableRunLoops)

  // .executeWithOptions(_.enableAutoCancelableRunLoops)

  /*val t = Task {
    println("Hi")
    TimeUnit.SECONDS.sleep(3)
  }.restartUntil(_ => false)
*/
  println("before")
  val c = t.executeAsync.loopForever.runToFuture  //.runAsync(_ => println("Done"))
  //t.runAsync(_ => println("Done"))
  println("after")

  import monix.execution.Scheduler.{global => scheduler}

  scheduler.scheduleOnce(10.seconds) {
    println("===> Do cancel")
    c.cancel()
  }

  // A Task instance that never completes
  /*val never = Task.never[Int]

  val timedOut = never.timeoutTo(3.seconds,
    Task.raiseError(new TimeoutException))

  timedOut.runAsync(r => println(r))*/

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
}