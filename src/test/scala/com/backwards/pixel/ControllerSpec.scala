package com.backwards.pixel

import java.util.concurrent.TimeUnit
import cats.implicits._
import monix.execution.schedulers.TestScheduler
import monocle.macros.syntax.lens._
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.duration._

class ControllerSpec extends AnyWordSpec with Matchers with OneInstancePerTest {
  implicit val scheduler: TestScheduler = TestScheduler()

  val config: Config.Static =
    Config.Static(maxScoreDelta = 5, maxWaitMs = 10)

  val noSideEffect: Output => Unit =
    _ => ()

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

  val `player 4 topdog`: Player =
    Player(ID(4, `0 elapsed ms`), Score(5))

  val `player 5 invisible`: Player =
    Player(ID(5, `0 elapsed ms`), Score(99))

  val triage = Map(
    Score(0) -> List(
      Waiting(`player 1 beginner`),
      Waiting(`player 2 beginner`)
    ),
    Score(3) -> List(
      Waiting(`player 3 advanced`)
    ),
    Score(5) -> List(
      Waiting(`player 4 topdog`)
    ),
    Score(99) -> List(
      Waiting(`player 5 invisible`)
    )
  )

  val controller = new Controller(config, noSideEffect)

  import controller._

  "Controller" should {
    "receive players in waiting" in {
      receive(Waiting(`player 1 beginner`))
      receive(Waiting(`player 2 beginner`))

      waitingQueueSnapshot mustBe List(Waiting(`player 1 beginner`), Waiting(`player 2 beginner`))
    }

    "triage empty queue of waiting players" in {
      val (triage, _) = controller.dequeueWaiting.run(Map.empty).value
      triage.isEmpty mustBe true
    }

    "triage waiting players that have been queued" in {
      receive(Waiting(`player 1 beginner`))
      receive(Waiting(`player 3 advanced`))

      val (triage, _) = controller.dequeueWaiting.run(Map.empty).value

      triage.size mustBe 2
      triage(`player 1 beginner`.score) mustBe List(Waiting(`player 1 beginner`))
      triage(`player 3 advanced`.score) mustBe List(Waiting(`player 3 advanced`))
    }

    "create a match (always in the same order) for two given players" in {
      val resultingMatch = Match(`player 1 beginner`, `player 2 beginner`)

      createMatch(`player 1 beginner`, `player 2 beginner`) mustBe resultingMatch
      createMatch(`player 2 beginner`, `player 1 beginner`) mustBe resultingMatch
    }

    "calculate score delta of two players" in {
      scoreDelta(`player 1 beginner`, `player 2 beginner`) mustBe 0
      scoreDelta(`player 1 beginner`, `player 3 advanced`) mustBe 3
    }

    "indicate if waiting player is overdue" in {
      val waiting = Waiting(`player 1 beginner`)

      overdue(waiting) mustBe false
      overdue(waiting.lens(_.elapsedMs).set(`> maxWaitMs elapsed`)) mustBe true
    }

    "aquire all waiting players for a given player" in {
      waitingsOfSameScore(`player 1 beginner`, triage) mustBe List(Waiting(`player 2 beginner`))
      waitingsWithinScoreDelta(`player 1 beginner`, triage) mustBe List(Waiting(`player 3 advanced`), Waiting(`player 4 topdog`))
    }

    "find match of same score for a waiting player" in {
      findMatch(Waiting(`player 1 beginner`), triage) mustBe Match(`player 1 beginner`, `player 2 beginner`).some
    }

    "not find match of same score for a waiting player" in {
      findMatch(Waiting(`player 4 topdog`), triage) mustBe None
    }

    "find match of different score within score delta for an overdue waiting player" in {
      val waiting = Waiting(`player 4 topdog`).lens(_.elapsedMs).set(`> maxWaitMs elapsed`)

      findMatch(waiting, triage) mustBe Match(`player 3 advanced`, `player 4 topdog`).some
    }

    "not find match of different score for an overdue waiting player when score delta is too big" in {
      val waiting = Waiting(`player 5 invisible`).lens(_.elapsedMs).set(`> maxWaitMs elapsed`)

      findMatch(waiting, triage) mustBe None
    }

    // TODO - findMatches: State[Triage, List[Match]]
  }

  "Controller managing triage of waiting players" should {
    def vacate(score: Score): Triage => Triage =
      _.updatedWith(score)(_ => Nil.some)

    "find all same score matches, which will also be evident in an updated triage" in {
      val (newTriage, matches) = findMatches(triage)

      newTriage mustBe vacate(Score(0))(triage)
      matches mustBe List(Match(`player 1 beginner`, `player 2 beginner`))
    }

    "find all matches both overdue and not, which will also be evident in an updated triage" in {
      val triageIncludingOverdue = triage.updatedWith(`player 3 advanced`.score) {
        _.map(_.map(_.lens(_.elapsedMs).set(`> maxWaitMs elapsed`)))
      }

      val (newTriage, matches) = findMatches(triageIncludingOverdue)

      newTriage mustBe (vacate(Score(0)) andThen vacate(Score(3)) andThen vacate(Score(5)))(triage)

      matches mustBe List(
        Match(`player 3 advanced`, `player 4 topdog`),
        Match(`player 1 beginner`, `player 2 beginner`)
      )
    }
  }


  ///////////

  "Controller OLD" should {
    "receive players in waiting" ignore {
      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      controller.waitingQueueSnapshot mustBe List(Waiting(`player 1 beginner`), Waiting(`player 2 beginner`))

      /*println(s"Waiting snapshot:")
      println(controller.waitingSnapshot.mkString("\n"))
      println()*/

      /*scheduler.scheduleOnce(7 seconds) {
        println("Hello, world!")
        controller.matching.cancel()
      }*/

      /*scheduler.tickOne()

      println("................ hi")

      TimeUnit.SECONDS.sleep(10)*/

      /*val (triage, matches) = controller.doMatch().run(Map.empty).value


      println(s"Triage:")
      println(triage.mkString("\n"))
      println()

      println(s"Waiting snapshot:")
      println(controller.waitingSnapshot.mkString("\n"))
      println()

      println(s"Final result:")
      println(matches)
      println()*/
    }

    "not find matches when there are no waiting players" ignore {
      controller.findMatches(Map.empty) mustBe (Map.empty, Nil)
    }

    "not find matches" in {
      val triage = Map(
        Score(0) -> List(
          Waiting(`player 1 beginner`),
          Waiting(`player 2 beginner`)
        )
      )

      controller.findMatches(triage) mustBe (triage, Nil)
    }

    "not find match" ignore {
      val triage = Map(
        Score(0) -> List(
          Waiting(`player 1 beginner`),
          Waiting(`player 2 beginner`)
        )
      )

      println("=====> " + controller.findMatch(Waiting(`player 1 beginner`), triage))
    }
  }



  "Controller matching via scheduler" should {
    "receive players in waiting" ignore {
      implicit val scheduler = monix.execution.Scheduler.global

      controller receive Waiting(`player 1 beginner`)
      controller receive Waiting(`player 2 beginner`)

      println(s"Waiting snapshot:")
      println(controller.waitingQueueSnapshot.mkString("\n"))
      println()


      scheduler.scheduleOnce(7 seconds) {
        println("Hello, world!")
        controller.matching.cancel()
      }

      println("Test waiting.......")
      TimeUnit.SECONDS.sleep(10)
    }
  }

  /*


  "Controller" should {
    "receive players in waiting" in {


      controller.waitingPlayersSnapshot.size mustBe 1
      controller.waitingPlayersSnapshot(`player 1 beginner`.score).size mustBe 2
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

  "Controller matching" should {
    "run in background on a scheduler" in {
      implicit val scheduler: TestScheduler = TestScheduler()

      val controller = new Controller(config, noSideEffect) {
        override def doMatch(): List[Match] = throw new Exception("Ran and prematurely ended")
      }

      controller.matching.isCompleted mustBe false
      scheduler.tickOne()
      controller.matching.isCompleted mustBe true
    }

    "match all waiting players of equal score" in {
      val numberOfScores = 10
      val numberOfPlayersPerScore = 10

      var blah = 0

      val player: Int => List[Player] = { score =>
        (1 to numberOfPlayersPerScore).map { _ =>
          blah = blah + 1
          Player(ID(blah, `0 elapsed ms`), Score(score))
        } toList
      }

      val players = (1 to numberOfScores).flatMap(player).toList
      players.map(Waiting.apply).foreach(controller.receive)

      controller.waitingPlayersSnapshot.size mustBe numberOfScores

      (1 to numberOfScores).foreach { score =>
        controller.waitingPlayersSnapshot(Score(score)).size mustBe numberOfPlayersPerScore
      }

      println(numberOfScores * numberOfPlayersPerScore / 2)
      controller.doMatch().size mustBe (numberOfScores * numberOfPlayersPerScore / 2)
    }
  }*/
}