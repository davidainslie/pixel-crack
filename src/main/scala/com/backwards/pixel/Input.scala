package com.backwards.pixel

import scala.math._
import cats.Show
import cats.syntax.all._
import monocle.macros.GenLens

sealed trait Input

final case class Waiting(player: Player, startedMs: Int, elapsedMs: () => Int) extends Input

object Waiting {
  def apply(player: Player): Waiting =
    new Waiting(player, player.id.elapsedMs(), player.id.elapsedMs)

  implicit val waitingShow: Show[Waiting] =
    Show.show[Waiting](_.player.show)
}

sealed abstract case class GameCompleted private(winner: Player, loser: Player) extends Input

object GameCompleted {
  def apply(winner: Player, loser: Player): GameCompleted = {
    def update(player: Player): Player = {
      val (scoreFactor, opponent) = player match {
        case `winner` => (0, loser)
        case `loser` => (1, winner)
      }

      // Equation in spec has been adjusted to not allow for a negative score - it seemed odd to see negative scores.
      val score = GenLens[Player](_.score).modify(s => max(0, round(atan(tan(s) + pow(-1, scoreFactor) * max(0.01, abs(s - opponent.score)))).toInt))
      val played = GenLens[Player](_.played).modify(_ + opponent)

      println(s"===> Before player = ${player.show}")
      val p = (score compose played)(player)
      println(s"===> After player = ${p.show}")
      p
    }

    new GameCompleted(update(winner), update(loser)) {}
  }

  implicit val gameCompletedShow: Show[GameCompleted] =
    Show.show[GameCompleted] { gameCompleted =>
      import gameCompleted._

      s"Game Completed: winner=${winner.id.show}, loser=${loser.id.show}"
    }
}