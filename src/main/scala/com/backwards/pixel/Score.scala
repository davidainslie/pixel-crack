package com.backwards.pixel

import scala.collection.immutable.Range
import scala.language.implicitConversions
import scala.runtime.RichInt
import cats.{Eq, Order}

case class Score(value: Int) extends AnyVal

object Score {
  implicit val scoreOrder: Order[Score] =
    (x: Score, y: Score) => x.value.compare(y.value)
}