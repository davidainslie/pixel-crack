package com.backwards.pixel

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentLinkedQueue, ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}
import scala.jdk.CollectionConverters._
import scala.util.Random
import monix.execution.Scheduler.{global => scheduler}

/** A simple (and dependency-free) driver to provide intuition for how the
 * controller might be called in an example production environment. NOTE, the
 * below is not representative of the type or style of solution we're looking
 * for. We also do not evaluate your solution using this driver and recommend
 * you do not rely on it for testing, evaluation, etc. */
class Driver(
  cf: (Output => Unit) => Controller,
  elapsedMs: () => Int,
  playersPerSec: Double,
  meanGameMs: Int,
  tickMs: Double,
  gamesPerPlayer: Int
) extends Runnable {
  val controller: Controller = cf(push)
  val games: ConcurrentLinkedQueue[Match] = new ConcurrentLinkedQueue[Match]
  val c: AtomicInteger = new AtomicInteger(0)
  //val t: Int = probabilisticRound(playersPerSec / 1000 * tickMs)
  val t = 2

  println(s"===> Init: games = ${games.iterator().asScala.toList}")

  def run(): Unit = {
    // Add new players. We add players at approximately PlayersPerSec.
    (1 to t).foreach(_ => controller.receive(createWaiting(c.getAndIncrement)))

    println(s"===> Running (added new players): games = $games")

    // Probabilistically expire games such that on average they run to MeanGameMs.
    val q = probabilisticRound(games.size() / meanGameMs * tickMs)

    println(s"===> q = $q")

    (1 to q).foreach { _ =>
      println(s"===> Running (lets poll): games = $games")

      games.poll() match {
        // Game to complete. We also decide whether to submit the players again, or whether they're quitting for the day.
        case m @ Match(a, b) =>
          println(s"===> Running (polled a match): match = $m, games = $games")

          val msg = if (Random.nextBoolean()) {
            GameCompleted(winner = a, b)
          } else {
            GameCompleted(winner = b, a)
          }

          controller receive msg

          if (!a.expired) controller receive Waiting(a)

          if (!b.expired) controller receive Waiting(b)

        // Possible as games may have changed from the assignment of `q`
        case null => ()
      }
    }
  }

  def shutdown(): Unit =
    controller.shutdown()

  private def push(out: Output): Unit = out match {
    case m: Match => games.add(m)
  }

  /** Create new player as `Waiting`. Accepts `i` as player's sequence number. */
  private def createWaiting(i: Int): Waiting = {
    // We overload the player ID as an expiry instant so we don't have to keep
    // track of it separately; and, we use the sequence number `i` as a means of
    // discriminating players who would otherwise expire at the same instant.
    val t: Int = elapsedMs() + gamesPerPlayer * meanGameMs
    val player = Player(ID(i.toLong << 32 | t & 0xFFFFFFFFL, elapsedMs), score = 0)

    Waiting(player)
  }

  private def probabilisticRound(input: Double): Int = {
    val f = Math.floor(input)
    val fDelta = input - f

    val res =
      if (fDelta > 0) {
        f + (if (fDelta > Random.nextDouble()) 1 else 0)
      } else {
        input
      }

    res.toInt
  }
}

object Driver extends App {
  // How many milliseconds are there between ticks?
  val tickRateMs = 5000

  // How many concurrent executions to allow? This would only be a factor if the per-tick execution is slower than the tick rate.
  val tPoolSize = 2

  // Setup controller, etc.
  val executor = new ScheduledThreadPoolExecutor(tPoolSize)
  val config = Config.Static(Math.PI, Int.MaxValue)

  val driver: Driver = new Driver(
    Controller(config)(scheduler),
    elapsedMs,
    playersPerSec = 100,
    meanGameMs = 30 * 1000,
    tickMs = tickRateMs,
    gamesPerPlayer = 20
  )

  // Run this crazy thing ...
  val fx: ScheduledFuture[_] = executor.scheduleAtFixedRate(driver, 0, tickRateMs, TimeUnit.MILLISECONDS)

  sys addShutdownHook {
    fx.cancel(true)
    driver.shutdown()
    executor.shutdown()
  }

  private def elapsedMs(): Int = {
    val v = System.currentTimeMillis() - executionStart

    if (v > Int.MaxValue)
      throw new AssertionError("This simulation has been running too long ...")

    v.toInt
  }
}
