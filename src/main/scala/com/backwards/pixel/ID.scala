package com.backwards.pixel

import cats.{Order, Show}

/** A player ID. */
final case class ID(value: Long, elapsedMs: () => Int) {
  def expired: Boolean = elapsedMs() > value
}

object ID {
  implicit val idOrder: Order[ID] =
    (x: ID, y: ID) => x.value.compare(y.value)

  implicit val idShow: Show[ID] =
    Show.show[ID](_.value.toString)
}