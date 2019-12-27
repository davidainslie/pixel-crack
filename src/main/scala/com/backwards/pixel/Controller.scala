package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import cats.Id
import cats.effect.IO
import fs2.concurrent.Queue
import fs2.{Pure, Stream}

/** We presume this `Controller` is managed somehow such that it is instantiated,
  * receives input somehow, and can appropriately process the output. The config
  * is liable to change during operation. There will be exactly one instance
  * of this controller created per-runtime (but `receive` could be called by
  * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit) {
  // TODO - Should this be a TMap ???
  val waiting: TrieMap[Score, Waiting] = new TrieMap[Score, Waiting]

  import cats.effect.{IO, ContextShift}
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val contextShift = IO.contextShift(global)

  val q = Queue.unbounded[IO, Int].unsafeRunSync()

  val x = Stream(Queue.unbounded[IO, Int])

  def blah: IO[Int] = IO(44)

  val s: Stream[IO, Int] = Stream.repeatEval(blah).map { i =>
    println(s"===> Oh ye, got a $i")
    TimeUnit.SECONDS.sleep(8)
    i
  }

  s.compile.drain.start.unsafeRunSync()

  // Over to you ...
  /*def receive(in: Input): Unit = {
    // println(s"Received $in")

    println(s"===> Controller: received = $in")
  }*/

  val receive: Input => Unit = {
    case w: Waiting =>
      waiting.put(w.player.score, w)

      //q.enqueue1(99)
      //i = i + 1


      /*waiting.values.foreach { player =>
        println(s"===> Player expired? ${player.expired}")
      }*/

      //println(q.dequeue.compile.last.unsafeRunSync())

      println(waiting.mkString("\n"))
  }
}