package com.backwards.pixel

import cats.implicits._
import monix.execution.schedulers.TestScheduler
import monocle.macros.syntax.lens._
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.ScalacheckShapeless._

class ControllerPropertySpec extends AnyWordSpec with Matchers with OneInstancePerTest with ScalaCheckDrivenPropertyChecks {
  implicit val scheduler: TestScheduler = TestScheduler()

  val config: Config.Static =
    Config.Static(maxScoreDelta = 5, maxWaitMs = 10)

  val noSideEffect: Output => Unit =
    _ => ()

  val controller = new Controller(config, noSideEffect)

  val `0 elapsed ms`: () => Int =
    () => 0

  val `> maxWaitMs elapsed`: () => Int =
    () => config.maxWaitMs + 1

  val `player 1 beginner`: Player =
    Player(ID(1, `0 elapsed ms`), Score(0))

  val `player 2 beginner`: Player =
    Player(ID(2, `0 elapsed ms`), Score(0))

  val `player 3 advanced`: Player =
    Player(ID(3, `0 elapsed ms`), Score(3))

  "Controller" should {
    "receive players in waiting" in {
      /*controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      controller.waitingPlayersSnapshot.size mustEqual 1
      controller.waitingPlayersSnapshot(`player 1 beginner`.score).size mustEqual 2
*/
      forAll { player: Player =>
        // Ensure foo has the required property
        //println(player)
        controller receive Waiting(player)

        println(controller.waitingPlayersSnapshot.mkString(",  "))
      }
    }
  }
}

/*
      val quitGen: Gen[String] = combine("q" or "Q", "u" or "U", "i" or "I", "t" or "T")

      val commandGen: Gen[String] = "q" or "Q" or quitGen

      forAll(commandGen) { command =>
        parseCommand(command) mustEqual Quit
      }
 */