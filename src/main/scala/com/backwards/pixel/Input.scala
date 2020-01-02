package com.backwards.pixel

import cats.Show
import cats.syntax.all._

sealed trait Input

final case class Waiting(player: Player, startedMs: Int, elapsedMs: () => Int) extends Input

object Waiting {
  def apply(player: Player): Waiting =
    new Waiting(player, player.id.elapsedMs(), player.id.elapsedMs)

  implicit val waitingShow: Show[Waiting] =
    Show.show[Waiting](_.player.show)
}

final case class GameCompleted(winner: Player, loser: Player) extends Input

object GameCompleted {
  implicit val gameCompletedShow: Show[GameCompleted] =
    Show.show[GameCompleted] { gameCompleted =>
      import gameCompleted._

      s"Game Completed: winner=${winner.id.show}, loser=${loser.id.show}"
    }
}