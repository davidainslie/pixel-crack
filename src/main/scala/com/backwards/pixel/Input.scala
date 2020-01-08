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

final case class GameCompleted private(winner: Player, loser: Player) extends Input

/**
 * {{{
 *  S_{new} = atan(tan(S_{old}) + (-1)^n * max(0.01, |S_{old}-S_{opponent}|)),
 *  where n=1 if they have lost and n=0 if they have won the game in question.
 * }}}
 */
object GameCompleted {
  def apply(winner: Player, loser: Player): GameCompleted = {
    def update(player: Player): Player = {
      val (scoreFactor, opponent) = if (player == winner) (0, loser) else (1, winner)

      val score = GenLens[Player](_.score).modify { s =>
        Score(max(0, atan(tan(s.toDouble) + pow(-1, scoreFactor) * max(0.01, abs(s.toDouble - opponent.score.toDouble)))))
      }

      val played = GenLens[Player](_.played).modify(_ + opponent.id)

      (score compose played)(player)
    }

    new GameCompleted(update(winner), update(loser))
  }

  implicit val gameCompletedShow: Show[GameCompleted] =
    Show.show[GameCompleted] { gameCompleted =>
      import gameCompleted._

      s"Game Completed: winner=${winner.id.show}(${winner.score.show}), loser=${loser.id.show}(${loser.score.show})"
    }
}