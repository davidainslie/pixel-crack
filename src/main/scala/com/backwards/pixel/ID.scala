package com.backwards.pixel

/** A player ID. */
final case class ID(value: Long)(val elapsedMs: () => Int) {
  def expired: Boolean = {
    val elapsed = elapsedMs()
    println(s"===> ID elapse check = $elapsed")
    elapsed > value
  }
}