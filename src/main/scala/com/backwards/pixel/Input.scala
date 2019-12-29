package com.backwards.pixel

sealed trait Input

final case class Waiting(player: Player, elapsedMs: () => Int) extends Input {
  val startedWaitingMs: Int = elapsedMs()
}

object Waiting {
  def apply(player: Player): Waiting =
    new Waiting(player, player.id.elapsedMs)
}

final case class GameCompleted(winner: Player, loser: Player) extends Input