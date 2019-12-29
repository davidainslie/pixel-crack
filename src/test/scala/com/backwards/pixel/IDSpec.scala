package com.backwards.pixel

import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IDSpec extends AnyWordSpec with Matchers {
  def elapsedMs(startMs: Int): () => Int = {
    val elapsedMs = new AtomicInteger(startMs)

    () => elapsedMs.getAndIncrement
  }

  "ID" should {
    "see elapsed milliseconds increment" in {
      val id = ID(1, elapsedMs(1))

      id.elapsedMs() mustEqual 1
      id.elapsedMs() mustEqual 2
      id.elapsedMs() mustEqual 3
    }

    "expire after elapsed milliseconds exceeds the value of ID" in {
      val id = ID(1, elapsedMs(1))

      id.expired mustEqual false
      id.expired mustEqual true
    }
  }
}