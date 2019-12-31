package com.backwards.pixel

sealed trait Output

final case class Match(playerA: Player, playerB: Player) extends Output
