package com.backwards.pixel

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.schedulers.TestScheduler
import org.scalatest.OneInstancePerTest
import monocle.macros.syntax.lens._
import cats.implicits._

class ControllerSpec extends AnyWordSpec with Matchers with OneInstancePerTest {
  spec =>

  implicit val scheduler: TestScheduler = TestScheduler()

  val controller = new Controller(Config.Static(0, 0), _ => ())

  val `5 elapsed ms`: () => Int =
    () => 5

  val `player 1 beginner`: Player =
    Player(ID(1, `5 elapsed ms`), Score(0))

  val `player 2 beginner`: Player =
    Player(ID(2, `5 elapsed ms`), Score(0))

  val `player 3 advanced`: Player =
    Player(ID(3, `5 elapsed ms`), Score(3))

  "Controller" should {
    "receive players in waiting" in {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      controller.waitingPlayersSnapshot.size mustEqual 1
      controller.waitingPlayersSnapshot(Score(0)).size mustEqual 2
    }

    /*"match two players of equal score" in {
      controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
      controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

      controller.doMatch() mustEqual Match(`player 1 expires in 5 milliseconds`, `player 2 expires in 15 milliseconds`)
    }*/

    "match player in waiting to another of equal score (lower ID comes first in a Match)" in {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      controller.findMatch(Waiting(`player 1 beginner`), `player 1 beginner`.score) mustBe Option(Match(`player 1 beginner`, `player 2 beginner`))
      controller.findMatch(Waiting(`player 2 beginner`), `player 2 beginner`.score) mustBe Option(Match(`player 1 beginner`, `player 2 beginner`))
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

    "match overdue player in waiting to another player of different score" in {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 3 advanced`)

      controller.findMatch(Waiting(`player 3 advanced`), `player 3 advanced`.score) mustBe Option(Match(`player 1 beginner`, `player 3 advanced`))
    }
  }
}