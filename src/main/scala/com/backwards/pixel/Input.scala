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

sealed abstract case class GameCompleted private(winner: Player, loser: Player) extends Input

object GameCompleted {
  def apply(winner: Player, loser: Player): GameCompleted = {
    def update(player: Player): Player = {
      val (opponent, scoreFactor) = player match {
        case `winner` => (loser, 1)
        case `loser` => (winner, 0)
      }

      val score = GenLens[Player](_.score).modify(s => atan(tan(s) + pow(-1, scoreFactor) * max(0.01, s - opponent.score)).toInt)
      val played = GenLens[Player](_.played).modify(_ :+ opponent) // TODO - NEED A SET

      println(s"===> Before player = ${player.show}")
      val p = (score compose played)(player)
      println(s"===> After player = ${p.show}")
      p
    }

    new GameCompleted(update(winner), update(loser)) {}
  }

  implicit val gameCompletedShow: Show[GameCompleted] =
    Show.show[GameCompleted] { gameCompleted =>
      import gameCompleted._

      s"Game Completed: winner=${winner.id.show}, loser=${loser.id.show}"
    }
}