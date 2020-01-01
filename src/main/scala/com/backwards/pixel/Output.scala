package com.backwards.pixel

import cats.Show
import cats.syntax.all._

sealed trait Output

final case class Match(playerA: Player, playerB: Player) extends Output

object Match {
  implicit val matchShow: Show[Match] =
    Show.show[Match](m => s"Match: ${m.playerA.show}, ${m.playerB.show}")
}