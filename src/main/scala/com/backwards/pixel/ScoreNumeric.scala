package com.backwards.pixel

import scala.collection.immutable.NumericRange
import scala.util.Try
import cats.implicits._

object ScoreNumeric extends ScoreNumeric

trait ScoreNumeric extends Numeric[Score] {
  implicit val scoreNumeric: ScoreNumeric = this

  override def plus(x: Score, y: Score): Score = Score(x.value + y.value)

  override def minus(x: Score, y: Score): Score = Score(x.value - y.value)

  override def times(x: Score, y: Score): Score = Score(x.value * y.value)

  override def negate(x: Score): Score = Score(x.value.bigDecimal.negate())

  override def fromInt(x: Int): Score = Score(x)

  override def parseString(str: String): Option[Score] = Try(BigDecimal(str)).toOption.map(Score.apply)

  override def toInt(x: Score): Int = x.value.toInt

  override def toLong(x: Score): Long = x.value.toLong

  override def toFloat(x: Score): Float = x.value.toFloat

  override def toDouble(x: Score): Double = x.value.toDouble

  override def compare(x: Score, y: Score): Int = x.compare(y)

  implicit class ScoreRange(score: Score) {
    def range(end: BigDecimal, f: (BigDecimal, BigDecimal, BigDecimal) => NumericRange[BigDecimal]): IndexedSeq[Score] =
      f(score.value, end, BigDecimal(0.01)).map(Score.apply)

    def to(end: Score): IndexedSeq[Score] =
      range(end.value, Range.BigDecimal.inclusive)

    def until(end: Score): IndexedSeq[Score] =
      range(end.value, Range.BigDecimal.apply)
  }
}