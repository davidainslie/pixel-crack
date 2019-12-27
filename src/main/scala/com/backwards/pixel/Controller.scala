package com.backwards.pixel

import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import cats.Id
import cats.effect.IO
import zio.{Queue, UIO, ZIO}

/** We presume this `Controller` is managed somehow such that it is instantiated,
  * receives input somehow, and can appropriately process the output. The config
  * is liable to change during operation. There will be exactly one instance
  * of this controller created per-runtime (but `receive` could be called by
  * multiple parallel threads for instance). */
class Controller private(config: Config, out: Output => Unit) {
  // TODO - Should this be a TMap ???
  val waiting: TrieMap[Score, Waiting] = new TrieMap[Score, Waiting]


  val q: UIO[Queue[Int]] = Queue.unbounded[Int]

  // Over to you ...
  /*def receive(in: Input): Unit = {
    // println(s"Received $in")

    println(s"===> Controller: received = $in")
  }*/

  val receive: Input => Unit = {
    case w: Waiting =>
      waiting.put(w.player.score, w)

      /*println("1")
      q.fl
      println("2")

      println(q.unsafeRunSync().dequeue.compile.last.unsafeRunSync())
*/
      //q.enqueue1(99)
      //i = i + 1


      /*waiting.values.foreach { player =>
        println(s"===> Player expired? ${player.expired}")
      }*/

      //println(q.dequeue.compile.last.unsafeRunSync())

      println(waiting.mkString("\n"))
  }
}

object Controller {
  def apply(config: Config): UIO[(Output => Unit) => Controller] = {
    //val q: UIO[Queue[Int]] = Queue.unbounded[Int]

    for {
      queue <- Queue.unbounded[Int]
      controller <- UIO(new Controller(config, _))
    } yield controller
  }
}