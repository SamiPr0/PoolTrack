package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwimDistanceTest {

  private val enteredAt = Instant.parse("2026-10-05T10:00:00Z")
  private val waiting = Entry(enteredAt.toEpochMilli(), awaitingDistance = true)

  @Test
  fun parseSwimDistance_acceptsWholeMetresWithinTheLimits() {
    assertEquals(1, parseSwimDistance("1"))
    assertEquals(1200, parseSwimDistance("1200"))
    assertEquals(1200, parseSwimDistance("  1200 "))
    assertEquals(MAX_SWIM_DISTANCE_METERS, parseSwimDistance("50000"))
  }

  @Test
  fun parseSwimDistance_rejectsEverythingElse() {
    assertNull(parseSwimDistance(""))
    assertNull(parseSwimDistance("   "))
    assertNull(parseSwimDistance("0"))
    assertNull(parseSwimDistance("000"))
    assertNull(parseSwimDistance("-5"))
    assertNull(parseSwimDistance("12.5"))
    assertNull(parseSwimDistance("12,5"))
    assertNull(parseSwimDistance("abc"))
    assertNull(parseSwimDistance("50001"))
    assertNull(parseSwimDistance("99999999999999999999"))
  }

  @Test
  fun formatSwimDistance_appendsTheUnit() {
    assertEquals("1200 m", formatSwimDistance(1200))
  }

  @Test
  fun isSwimPending_isTrueOnlyForAnEntryAwaitingItsDistance() {
    assertTrue(isSwimPending(waiting))
    assertFalse(isSwimPending(null))
    assertFalse(isSwimPending(waiting.copy(awaitingDistance = false)))
    assertFalse(isSwimPending(waiting.copy(awaitingDistance = false, swimDistanceMeters = 800)))
  }

  @Test
  fun remainingCancelWindow_countsDownFromTenMinutes() {
    assertEquals(CANCEL_WINDOW, remainingCancelWindow(waiting, enteredAt))
    assertEquals(
        Duration.ofMinutes(4),
        remainingCancelWindow(waiting, enteredAt.plus(Duration.ofMinutes(6))),
    )
    assertEquals(
        Duration.ofSeconds(1),
        remainingCancelWindow(waiting, enteredAt.plus(CANCEL_WINDOW).minusSeconds(1)),
    )
  }

  @Test
  fun remainingCancelWindow_isNull_exactlyWhenTenMinutesPassed() {
    assertNull(remainingCancelWindow(waiting, enteredAt.plus(CANCEL_WINDOW)))
    assertNull(remainingCancelWindow(waiting, enteredAt.plus(Duration.ofDays(3))))
  }

  @Test
  fun isLogReminderDue_isFalse_insideTheCancelWindow() {
    assertFalse(isLogReminderDue(waiting, enteredAt.plus(Duration.ofMinutes(9))))
  }

  @Test
  fun isLogReminderDue_isTrue_fromTheEndOfTheCancelWindow_withoutAnUpperLimit() {
    assertTrue(isLogReminderDue(waiting, enteredAt.plus(CANCEL_WINDOW)))
    assertTrue(isLogReminderDue(waiting, enteredAt.plus(Duration.ofDays(2))))
  }

  @Test
  fun isLogReminderDue_isFalse_withoutAnEntryAwaitingADistance() {
    val later = enteredAt.plus(Duration.ofHours(1))
    assertFalse(isLogReminderDue(null, later))
    assertFalse(isLogReminderDue(waiting.copy(awaitingDistance = false), later))
  }
}
