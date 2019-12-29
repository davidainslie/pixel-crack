package com.backwards.pixel

import cats.Order

/** A player ID. */
final case class ID(value: Long)(val elapsedMs: () => Int) {
  def expired: Boolean = {
    val elapsed = elapsedMs()
    println(s"===> ID elapse check = $elapsed")
    elapsed > value
  }
}

object ID {
  implicit val idOrder: Order[ID] =
    (x: ID, y: ID) => x.value.compare(y.value)
}