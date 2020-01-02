package com.backwards.pixel

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.syntax.all._
import monocle.macros.GenLens

class GameCompletedSpec extends AnyWordSpec with Matchers {
  val `0 elapsed ms`: () => Int =
    () => 0

  def score(score: Int) = GenLens[Player](_.score).set(score)

  def played(opponent: Player) = GenLens[Player](_.played).modify(_ + opponent)

  "Game completed" should {
    "update winning and losing players" in {
      val player1: Player =
        Player(ID(1, `0 elapsed ms`), score = 0)

      val player2: Player =
        Player(ID(2, `0 elapsed ms`), score = 99)

      val GameCompleted(player2Wins, player1Loses) = GameCompleted(winner = player2, loser = player1)
      player2Wins mustBe (score(2) compose played(player1Loses))(player2)

      val GameCompleted(player1Wins, player2Loses) = GameCompleted(winner = player1, loser = player2)



      println(player2Wins.show)
      println(player2Loses.show)

      println("\n")

      println(player1Wins.show)
      println(player1Loses.show)
    }
  }
}