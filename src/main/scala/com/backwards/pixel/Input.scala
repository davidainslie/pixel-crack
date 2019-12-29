package com.backwards.pixel

sealed trait Input

final case class Waiting(player: Player, elapsedMs: () => Int) extends Input {
  val startedWaitingMs: Int = elapsedMs()
}

final case class GameCompleted(winner: Player, loser: Player) extends Input