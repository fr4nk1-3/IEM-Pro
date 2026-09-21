package com.example

import com.example.model.ChannelState
import com.example.model.MixBusState
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun busLimiter_clampsFaderCeiling() {
    val bus = MixBusState(
      id = 1,
      name = "IEM 1",
      limiterActive = true,
      limiterThresholdDb = -6f,
      masterLevel = 0.8f
    )

    // With limiterActive and threshold -6dB, max fader level should be dbToFader(-6f) = 0.60f
    val maxFader = bus.getMaxFaderLevel()
    assertEquals(0.60f, maxFader, 0.01f)

    // Master level when clamped to maxFader cannot exceed 0.60f
    val clamped = 0.95f.coerceAtMost(maxFader)
    assertEquals(0.60f, clamped, 0.01f)

    // When limiter is disabled, max fader is 1.0f
    val unmutedLimiterBus = bus.copy(limiterActive = false)
    assertEquals(1.0f, unmutedLimiterBus.getMaxFaderLevel(), 0.001f)
  }

  @Test
  fun dbToFaderAndFaderToDb_areConsistent() {
    val testDbs = listOf(0f, -6f, -10f, -20f, -30f, -50f)
    for (db in testDbs) {
      val fader = ChannelState.dbToFader(db)
      val convertedDb = ChannelState.faderToDb(fader)
      assertEquals("DB mismatch for $db dB", db, convertedDb, 0.05f)
    }
  }
}
