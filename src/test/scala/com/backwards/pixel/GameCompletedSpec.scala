package com.backwards.pixel

import monocle.macros.GenLens
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The assertions in this test were taken after running the code since I did not know what the spec's score equation would generate.
 */
class GameCompletedSpec extends AnyWordSpec with Matchers {
  val `0 elapsed ms`: () => Int =
    () => 0

  val player1: Player =
    Player(ID(1, `0 elapsed ms`), Score(0))

  val player2: Player =
    Player(ID(2, `0 elapsed ms`), Score(0.03))

  def score(score: Score): Player => Player =
    GenLens[Player](_.score).set(score)

  def played(opponent: Player): Player => Player =
    GenLens[Player](_.played).modify(_ + opponent.id)

  "Game completed" should {
    "update players - player1 WINS, player2 LOSES" in {
      val GameCompleted(player1Wins, player2Loses) = GameCompleted(winner = player1, loser = player2)

      player1Wins  mustBe (score(Score(0.03)) compose played(player2Loses))(player1)
      player2Loses mustBe (score(Score(0)) compose played(player1Wins))(player2)
    }

    "update players - player1 LOSES, player2 WINS" in {
      val GameCompleted(player2Wins, player1Loses) = GameCompleted(winner = player2, loser = player1)

      player1Loses mustBe (score(Score(0)) compose played(player2Wins))(player1)
      player2Wins  mustBe (score(Score(0.06)) compose played(player1Loses))(player2)
    }
  }
}