package com.backwards.pixel

import monocle.macros.GenLens
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The assertions in this test were taken after running the code since I did not know what the spec's score equation would generate.
 * Within the code, after a few runs, I noticed that the equation could generate a negative score which didn't seem correct,
 * so I have ensured that zero is the lowest score as this is the starting score for any player.
 */
class GameCompletedSpec extends AnyWordSpec with Matchers {
  val `0 elapsed ms`: () => Int =
    () => 0

  def score(score: Int): Player => Player =
    GenLens[Player](_.score).set(score)

  def played(opponent: Player): Player => Player =
    GenLens[Player](_.played).modify(_ + opponent.id)

  "Game completed" should {
    "update winning and losing players" in {
      val player1: Player =
        Player(ID(1, `0 elapsed ms`), score = 0)

      val player2: Player =
        Player(ID(2, `0 elapsed ms`), score = 99)

      val GameCompleted(player2Wins, player1Loses) = GameCompleted(winner = player2, loser = player1)
      player2Wins mustBe (score(2) compose played(player1Loses))(player2)
      player1Loses mustBe played(player2Wins)(player1)

      val GameCompleted(player1Wins, player2Loses) = GameCompleted(winner = player1, loser = player2)
      player1Wins mustBe (score(2) compose played(player2Loses))(player1)
      player2Loses mustBe (score(0) compose played(player1Wins))(player2)
    }
  }
}