package com.backwards.pixel

/** Config is provided through this interface and would be liable to change at runtime. */
trait Config {

  def maxScoreDelta: Double

  def maxWaitMs: Int

}

object Config {
  case class Static(
    override val maxScoreDelta: Double,
    override val maxWaitMs: Int) extends Config
}
