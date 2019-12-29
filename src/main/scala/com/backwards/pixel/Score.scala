package com.backwards.pixel

import cats.implicits._
import cats.{Monoid, Order}

// TODO - Smart constructor or Refined to avoid possible negative
case class Score(value: Int) extends AnyVal {
  def decrement: Option[Score] = {
    val nextValue = value - 1

    if (nextValue >= 0)
      Option(Score(nextValue))
    else
      None
  }
}

object Score {
  implicit val scoreMonoid: Monoid[Score] =
    Monoid.instance[Score](Score(0), (s1, s2) => Score(s1.value |+| s2.value))

  implicit val scoreOrder: Order[Score] =
    (x: Score, y: Score) => x.value.compare(y.value)
}