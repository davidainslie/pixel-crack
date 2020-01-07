package com.backwards.pixel

import cats.implicits._
import cats.{Order, Show}
import com.backwards.pixel.Score._

final case class Score private(value: BigDecimal) extends AnyVal {
  def increment: Score =
    Score(value + new java.math.BigDecimal(1).movePointLeft(precision))

  def toDouble: Double = value.toDouble
}

object Score {
  val precision: Int = 2

  def apply(value: Double): Score = apply(BigDecimal(value))

  def apply(value: BigDecimal): Score = new Score(value.setScale(precision, BigDecimal.RoundingMode.HALF_UP))

  implicit val scoreOrder: Order[Score] =
    (x: Score, y: Score) => x.value.compare(y.value)

  implicit val scoreShow: Show[Score] =
    Show.show[Score](_.value.toString)
}