package com.backwards.pixel

import scala.collection.concurrent.TrieMap

/** We presume this `Controller` is managed somehow such that it is instantiated,
  * receives input somehow, and can appropriately process the output. The config
  * is liable to change during operation. There will be exactly one instance
  * of this controller created per-runtime (but `receive` could be called by
  * multiple parallel threads for instance). */
class Controller(config: Config, out: Output => Unit) {
  // TODO - Should this be a TMap ???
  val waiting: TrieMap[Score, Waiting] = new TrieMap[Score, Waiting]

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

      println(waiting.mkString("\n"))
  }
}