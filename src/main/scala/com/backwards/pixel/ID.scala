package com.backwards.pixel

import cats.Order

/** A player ID. */
final case class ID(value: Long, elapsedMs: () => Int) {
  def expired: Boolean = elapsedMs() > value
}

object ID {
  implicit val idOrder: Order[ID] =
    (x: ID, y: ID) => x.value.compare(y.value)
}