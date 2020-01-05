package com.backwards.pixel

import scala.collection.mutable.ListBuffer
import cats.data.State
import cats.effect.concurrent.Ref
import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, Fiber, IO, Timer}
import cats.implicits._
import monocle.macros.syntax.lens._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Inspectors, OneInstancePerTest}

class ControllerSpec extends AnyWordSpec with Matchers with OneInstancePerTest with Inspectors with MockitoSugar {
  val ec: TestContext = TestContext()
  implicit val contextShift: ContextShift[IO] = ec.contextShift[IO]
  implicit val timer: Timer[IO] = IO.timer(ec)

  val config: Config.Static =
    Config.Static(maxScoreDelta = 5, maxWaitMs = 10)

  val issuedMatches: ListBuffer[Match] = new ListBuffer[Match]()

  val issueMatchEvent: Output => Unit = {
    case m: Match => issuedMatches += m
  }

  val `0 elapsed ms`: () => Int =
    () => 0

  val `> maxWaitMs elapsed`: () => Int =
    () => config.maxWaitMs + 1

  val `player 1 beginner`: Player =
    Player(ID(1, `0 elapsed ms`), score = 0)

  val `player 2 beginner`: Player =
    Player(ID(2, `0 elapsed ms`), score = 0)

  val `player 3 advanced`: Player =
    Player(ID(3, `0 elapsed ms`), score = 3)

  val `player 4 topdog`: Player =
    Player(ID(4, `0 elapsed ms`), score = 5)

  val `player 5 invisible`: Player =
    Player(ID(5, `0 elapsed ms`), score = 99)

  val triage = Map(
    0 -> List(
      Waiting(`player 1 beginner`),
      Waiting(`player 2 beginner`)
    ),
    3 -> List(
      Waiting(`player 3 advanced`)
    ),
    5 -> List(
      Waiting(`player 4 topdog`)
    ),
    99 -> List(
      Waiting(`player 5 invisible`)
    )
  )

  val controller: Controller = new Controller(config, issueMatchEvent) {
    override def startMatching(matching: IO[(Triage, List[Match])]): Fiber[IO, (Triage, List[Match])] =
      mock[Fiber[IO, (Triage, List[Match])]]
  }

  import controller._

  "Controller" should {
    "receive players in waiting" in {
      receive(Waiting(`player 1 beginner`))
      receive(Waiting(`player 2 beginner`))

      waitingQueueSnapshot mustBe List(Waiting(`player 1 beginner`), Waiting(`player 2 beginner`))
    }

    "triage waiting players" in {
      val waitings = List(Waiting(`player 1 beginner`), Waiting(`player 3 advanced`))
      val triage = add(waitings)(Map.empty)

      triage.size mustBe 2
      triage(`player 1 beginner`.score) mustBe List(Waiting(`player 1 beginner`))
      triage(`player 3 advanced`.score) mustBe List(Waiting(`player 3 advanced`))
    }

    // TODO - canPlay ??? Missed this test!

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
  }

  "Controller managing triage of waiting players" should {
    def vacate(score: Score): Triage => Triage =
      _.updatedWith(score)(_ => Nil.some)

    "find all same score matches, which will also be evident in an updated triage" in {
      val (newTriage, matches) = findMatches(triage)

      newTriage mustBe vacate(0)(triage)
      forAll(List(matches, issuedMatches))(_ mustBe List(Match(`player 1 beginner`, `player 2 beginner`)))
    }

    "find all matches both overdue and not, which will also be evident in an updated triage" in {
      val triageIncludingOverdue = triage.updatedWith(`player 3 advanced`.score) {
        _.map(_.map(_.lens(_.elapsedMs).set(`> maxWaitMs elapsed`)))
      }

      val (newTriage, matches) = findMatches(triageIncludingOverdue)

      newTriage mustBe (vacate(0) andThen vacate(3) andThen vacate(5))(triage)

      matches mustBe List(
        Match(`player 3 advanced`, `player 4 topdog`),
        Match(`player 1 beginner`, `player 2 beginner`)
      )

      forAll(List(matches, issuedMatches))(_ mustBe List(
        Match(`player 3 advanced`, `player 4 topdog`),
        Match(`player 1 beginner`, `player 2 beginner`)
      ))
    }
  }

  "Controller matching (daemon) task" should {
    "match a bunch of players" in {
      val numberOfScores = 10
      val numberOfPlayersPerScore = 10

      def createPlayers(score: Int): State[List[Player], Unit] =
        for {
          _ <- State.modify[List[Player]] {
            _ ++ (score until score + numberOfPlayersPerScore).map(id => Player(ID(id, `0 elapsed ms`), score))
          }
          _ <- if (score >= numberOfScores) State.get[List[Player]] else createPlayers(score + 1)
        } yield ()

      val (players, _) = createPlayers(1).run(List.empty[Player]).value

      players.map(Waiting.apply).foreach(receive)

      val (triage, matches) = doMatch(Ref.unsafe[IO, Triage](Map.empty)).unsafeRunSync

      triage.values.flatten mustBe Nil
      matches.size mustBe (numberOfScores * numberOfPlayersPerScore / 2)
    }
  }
}