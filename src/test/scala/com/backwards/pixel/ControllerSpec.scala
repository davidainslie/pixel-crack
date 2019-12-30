package com.backwards.pixel

import cats.implicits._
import monix.execution.schedulers.TestScheduler
import monocle.macros.syntax.lens._
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ControllerSpec extends AnyWordSpec with Matchers with OneInstancePerTest {
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
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      controller.waitingPlayersSnapshot.size mustEqual 1
      controller.waitingPlayersSnapshot(`player 1 beginner`.score).size mustEqual 2
    }

    "match player in waiting to another of equal score (and vice versa)" in {
      def findMatch(playerA: Player, playerB: Player): Option[Match] = {
        controller receive Waiting(playerA)
        controller receive Waiting(playerB)

        val foundMatch = controller.findMatch(Waiting(playerA), playerA.score)

        // 2nd call to finding a match will not "match" as the match has already been created
        controller.findMatch(Waiting(playerA), playerA.score) mustBe None

        foundMatch
      }

      findMatch(`player 1 beginner`, `player 2 beginner`) mustBe Option(Match(`player 1 beginner`, `player 2 beginner`))
      findMatch(`player 2 beginner`, `player 1 beginner`) mustBe Option(Match(`player 1 beginner`, `player 2 beginner`))
    }

    "match player in waiting to another of lesser score" in {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 3 advanced`)

      controller.findMatch(Waiting(`player 3 advanced`), `player 1 beginner`.score) mustBe Option(Match(`player 1 beginner`, `player 3 advanced`))
    }

    "not match player in waiting as there are no other players" in {
      controller receive Waiting(`player 1 beginner`)

      controller.findMatch(Waiting(`player 1 beginner`), `player 1 beginner`.score) mustBe None
    }

    "not match non overdue player in waiting to another player of different score" in {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 3 advanced`)

      controller.findMatch(Waiting(`player 3 advanced`), `player 3 advanced`.score) mustBe None
    }

    "match overdue player in waiting to another player of lesser score" in {
      val `player 3 advanced waiting` = Waiting(`player 3 advanced`, 0, `> maxWaitMs elapsed`)

      controller receive Waiting(`player 1 beginner`)
      controller receive `player 3 advanced waiting`

      controller.findMatch(`player 3 advanced waiting`, `player 3 advanced`.score) mustBe Option(Match(`player 1 beginner`, `player 3 advanced`))
    }

    "not match overdue player in waiting to another of lesser score as the score delta exceeds configured 'maximum score delta'" in {
      val controller = new Controller(config.lens(_.maxScoreDelta).set(1), noSideEffect)

      val `player 3 advanced waiting` = Waiting(`player 3 advanced`, 0, `> maxWaitMs elapsed`)

      controller receive Waiting(`player 1 beginner`)
      controller receive `player 3 advanced waiting`

      controller.findMatch(`player 3 advanced waiting`, `player 3 advanced`.score) mustBe None
    }
  }


  /*"match two players of equal score" in {
    controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
    controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

    controller.doMatch() mustEqual Match(`player 1 expires in 5 milliseconds`, `player 2 expires in 15 milliseconds`)
  }*/

  "Controller matching" should {
    "run in background on a scheduler" in {
      val controller = new Controller(config, noSideEffect) {
        override def doMatch(): List[Match] = throw new Exception("Ran and prematurely ended")
      }

      controller.matching.isCompleted mustBe false
      scheduler.tickOne()
      controller.matching.isCompleted mustBe true
    }

    /*"match all waiting players of equal score" in {

    }*/
  }
}