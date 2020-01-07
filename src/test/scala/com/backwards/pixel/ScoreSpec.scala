package com.backwards.pixel

import monocle.Iso
import org.scalacheck.Gen
import org.scalacheck.Gen.Choose
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import com.backwards.pixel.ScoreNumeric._

class ScoreSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  "Score" should {
    "be isomorphic to Double of 2 decimal places" in {
      val iso = Iso[Double, Score](Score.apply)(_.value.toDouble)

      val doubleGen: Gen[Double] =
        Choose.chooseDouble.choose(0d, 100d).map(d => f"$d%3.2f".toDouble)

      forAll(doubleGen) { d =>
        iso.reverseGet(iso.get(d)) mustBe d
      }
    }

    "be numeric" in {
      Score(0) + Score(2) mustBe Score(2)
      Score(1.723) + Score(5.2) mustBe Score(6.92)
    }
  }
}