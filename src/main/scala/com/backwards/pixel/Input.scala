package com.backwards.pixel

sealed trait Input

final case class Waiting(player: Player, startedWaitingMs: Int) extends Input

final case class GameCompleted(winner: Player, loser: Player) extends Input