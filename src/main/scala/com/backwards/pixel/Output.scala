package com.backwards.pixel

sealed trait Output

case class Match(playerA: Player, playerB: Player) extends Output
