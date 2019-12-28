package com.backwards.pixel

import java.util.concurrent.TimeUnit
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.schedulers.TestScheduler

class ControllerSpec extends AnyWordSpec with Matchers {
  implicit val scheduler: TestScheduler = TestScheduler()

  val `player 1 expires in 5 milliseconds`: Player =
    Player(ID(1)(() => 5), Score(0))

  val `player 2 expires in 15 milliseconds`: Player =
    Player(ID(2)(() => 5), Score(0))

  "Controller" should {
    "receive players in waiting" in {
      val controller = new Controller(Config.Static(0, 0), _ => ())

      controller.receive(Waiting(`player 1 expires in 5 milliseconds`))
      controller.receive(Waiting(`player 2 expires in 15 milliseconds`))

      controller.playersInWaiting.size mustEqual 2
    }

  }
}