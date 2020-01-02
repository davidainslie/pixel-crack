package com.backwards.pixel

import cats.Show
import cats.syntax.all._

final case class Player(id: ID, score: Int, played: Set[Player] = Set.empty) {
  def expired: Boolean = id.expired
}

object Player {
  implicit val playerShow: Show[Player] =
    Show.show[Player] { player =>
      import player._

      val hasPlayed = played.map(_.id.show) match {
        case ps if ps.isEmpty => "nobody"
        case ps => ps.mkString(", ")
      }

      s"Player: id=${id.show}, score=$score, played=$hasPlayed"
    }
}