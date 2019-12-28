package com.backwards.pixel

import java.util.concurrent.TimeUnit
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.schedulers.TestScheduler
import org.scalatest.OneInstancePerTest

class ControllerSpec extends AnyWordSpec with Matchers with OneInstancePerTest {
  implicit val scheduler: TestScheduler = TestScheduler()

  val controller = new Controller(Config.Static(0, 0), _ => ())

  val `player 1 expires in 5 milliseconds`: Player =
    Player(ID(1)(() => 5), Score(0))

  val `player 2 expires in 15 milliseconds`: Player =
    Player(ID(2)(() => 5), Score(0))

  "Controller" should {
    "receive players in waiting" in {
      controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
      controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

      controller.waitingPlayersSnapshot.size mustEqual 1
      controller.waitingPlayersSnapshot(Score(0)).size mustEqual 2
    }

    "match two players of equal score" in {
      controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
      controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

      controller.doMatch() mustEqual Match(`player 1 expires in 5 milliseconds`, `player 2 expires in 15 milliseconds`)
    }
  }
}