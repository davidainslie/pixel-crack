package com.backwards.pixel

import cats.implicits._
import cats.{Monoid, Order, Show}
import scala.math.abs

// TODO - Smart constructor or Refined to avoid possible negative.
// TODO - However, the use of this ADT has been removed (maybe good idea or maybe not)
final case class Score private(value: BigDecimal) extends AnyVal {
  /*def decrement: Option[Score] = {
    val nextValue = value - 1

    if (nextValue >= 0)
      Option(Score(nextValue))
    else
      None
  }*/

  def difference(other: Score): Score =
    Score(abs(value.toDouble - other.value.toDouble))

  def minus(other: Score): Score =
    Score(value.toDouble - other.value.toDouble)
}

// println(BigDecimal(1.23456789).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)

object Score {
  def apply(value: Double): Score = new Score(BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP))
    //throw new NotImplementedError

  /*implicit val scoreMonoid: Monoid[Score] =
    Monoid.instance[Score](Score(0), (s1, s2) => Score(s1.value |+| s2.value))
*/

  implicit val scoreOrder: Order[Score] =
    (x: Score, y: Score) => x.value.compare(y.value)

  implicit val scoreShow: Show[Score] =
    Show.show[Score](_.value.toString)
}