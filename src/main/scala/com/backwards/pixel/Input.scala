package com.backwards.pixel

sealed trait Input

final case class Waiting(player: Player) extends Input

final case class GameCompleted(winner: Player, loser: Player) extends Input