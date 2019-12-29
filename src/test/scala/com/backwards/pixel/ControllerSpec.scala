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

  val `player 1 beginner`: Waiting =
    Waiting(Player(ID(1)(() => 5), Score(0)))

  val `player 2 beginner`: Waiting =
    Waiting(Player(ID(2)(() => 5), Score(0)))

  val `player 3 advanced`: Waiting =
    Waiting(Player(ID(2)(() => 5), Score(3)))

  "Controller" should {
    "receive players in waiting" in {
      controller receive `player 1 beginner`
      controller receive `player 2 beginner`

      controller.waitingPlayersSnapshot.size mustEqual 1
      controller.waitingPlayersSnapshot(Score(0)).size mustEqual 2
    }

    /*"match two players of equal score" in {
      controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
      controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

      controller.doMatch() mustEqual Match(`player 1 expires in 5 milliseconds`, `player 2 expires in 15 milliseconds`)
    }*/

    "match player in waiting to another of equal score (lower ID comes first in a Match)" in {
      controller receive `player 1 beginner`
      controller receive `player 2 beginner`

      controller doMatch `player 1 beginner` mustBe Option(Match(`player 1 beginner`.player, `player 2 beginner`.player))
      controller doMatch `player 2 beginner` mustBe Option(Match(`player 1 beginner`.player, `player 2 beginner`.player))
    }

    "not match player in waiting as there are no other players" in {
      controller receive `player 1 beginner`

      controller doMatch `player 1 beginner` mustBe None
    }

    "not match non overdue player in waiting to another player of different score" in {
      controller receive `player 1 beginner`
      controller receive `player 3 advanced`

      controller doMatch `player 3 advanced` mustBe None
    }

    "match overdue player in waiting to another player of different score" in {
      controller receive `player 1 beginner`
      controller receive `player 3 advanced`

      controller doMatch `player 3 advanced` mustBe Option(Match(`player 1 beginner`.player, `player 3 advanced`.player))
    }
  }
}