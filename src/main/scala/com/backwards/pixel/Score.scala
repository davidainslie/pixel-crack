package com.backwards.pixel

import scala.language.implicitConversions
import cats.implicits._
import cats.{Monoid, Order}

case class Score(value: Int) extends AnyVal

object Score {
  implicit val scoreMonoid: Monoid[Score] =
    Monoid.instance[Score](Score(0), (s1, s2) => Score(s1.value |+| s2.value))

  implicit val scoreOrder: Order[Score] =
    (x: Score, y: Score) => x.value.compare(y.value)
}