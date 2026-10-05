package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwimDurationTest {

  private val enteredAt = Instant.parse("2026-10-05T10:00:00Z")
  private val entry = Entry(timestampEpochMilli = enteredAt.toEpochMilli())

  @Test
  fun swimDurationToReport_isNull_whenThereIsNoEntry() {
    assertNull(swimDurationToReport(null, enteredAt.plus(Duration.ofHours(1))))
  }

  @Test
  fun swimDurationToReport_isNull_whenUnlockedBeforeTheMinimum() {
    assertNull(swimDurationToReport(entry, enteredAt.plus(Duration.ofMinutes(9).plusSeconds(59))))
  }

  @Test
  fun swimDurationToReport_isTheElapsedTime_atTheMinimum() {
    assertEquals(MIN_SWIM_DURATION, swimDurationToReport(entry, enteredAt.plus(MIN_SWIM_DURATION)))
  }

  @Test
  fun swimDurationToReport_isTheElapsedTime_afterTheMinimum() {
    val now = enteredAt.plus(Duration.ofMinutes(72))

    assertEquals(Duration.ofMinutes(72), swimDurationToReport(entry, now))
  }

  @Test
  fun swimDurationToReport_isNull_whenAlreadyReported() {
    val reported = entry.copy(swimDurationMillis = 1L)

    assertNull(swimDurationToReport(reported, enteredAt.plus(Duration.ofHours(1))))
  }

  @Test
  fun swimDurationToReport_isNull_whenTheEntryIsTooOld() {
    assertNull(swimDurationToReport(entry, enteredAt.plus(MAX_SWIM_DURATION).plusSeconds(1)))
  }

  @Test
  fun isSwimPending_isTrue_forAFreshUnreportedEntry() {
    assertTrue(isSwimPending(entry, enteredAt))
    assertTrue(isSwimPending(entry, enteredAt.plus(MAX_SWIM_DURATION)))
  }

  @Test
  fun isSwimPending_isFalse_withoutEntryOrOnceReportedOrTooOld() {
    assertFalse(isSwimPending(null, enteredAt))
    assertFalse(isSwimPending(entry.copy(swimDurationMillis = 1L), enteredAt))
    assertFalse(isSwimPending(entry, enteredAt.plus(MAX_SWIM_DURATION).plusSeconds(1)))
  }

  @Test
  fun formatSwimDuration_showsMinutesOnly_underAnHour() {
    assertEquals("45min", formatSwimDuration(Duration.ofMinutes(45)))
  }

  @Test
  fun formatSwimDuration_showsHoursAndMinutes_fromAnHour() {
    assertEquals("1h 12min", formatSwimDuration(Duration.ofMinutes(72)))
    assertEquals("2h 0min", formatSwimDuration(Duration.ofHours(2)))
  }
}
