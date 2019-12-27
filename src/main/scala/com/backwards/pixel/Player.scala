package com.backwards.pixel

final case class Player(id: ID, score: Score, played: List[Player] = Nil) {
  def expired: Boolean = id.expired
}