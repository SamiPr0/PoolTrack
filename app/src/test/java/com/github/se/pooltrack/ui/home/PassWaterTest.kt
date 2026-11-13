package com.github.se.pooltrack.ui.home

import com.github.se.pooltrack.model.subscription.Subscription
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassWaterTest {

  private val now = Instant.ofEpochMilli(1_000_000)

  private fun pass(added: Long = 0, expires: Long? = null, maxEntries: Int? = null) =
      Subscription(
          id = "a",
          uri = "content://a",
          displayName = "a",
          addedAtEpochMilli = added,
          expiresAtEpochMilli = expires,
          maxEntries = maxEntries,
      )

  @Test
  fun datedPass_emptiesWithTime() {
    val level = passWaterLevel(pass(added = 0, expires = 4_000_000), usedEntries = 0, now = now)

    assertEquals(0.75f, level!!, 0.0001f)
  }

  @Test
  fun datedPass_isEmptyOnceExpired_andFullBeforeItStarts() {
    assertEquals(0f, passWaterLevel(pass(added = 0, expires = 500_000), 0, now)!!, 0f)
    assertEquals(1f, passWaterLevel(pass(added = 2_000_000, expires = 3_000_000), 0, now)!!, 0f)
  }

  @Test
  fun datedPass_withNoLength_isFullUntilItExpires_thenEmpty() {
    assertEquals(1f, passWaterLevel(pass(added = 2_000_000, expires = 2_000_000), 0, now)!!, 0f)
    assertEquals(0f, passWaterLevel(pass(added = 500_000, expires = 500_000), 0, now)!!, 0f)
  }

  @Test
  fun entryLimitedPass_emptiesWithEachEntry() {
    assertEquals(0.7f, passWaterLevel(pass(maxEntries = 10), usedEntries = 3, now = now)!!, 0.0001f)
  }

  @Test
  fun entryLimitedPass_neverGoesBelowEmpty() {
    assertEquals(0f, passWaterLevel(pass(maxEntries = 2), usedEntries = 5, now = now)!!, 0f)
  }

  @Test
  fun entryLimitedPass_withNoEntries_isEmpty() {
    assertEquals(0f, passWaterLevel(pass(maxEntries = 0), usedEntries = 0, now = now)!!, 0f)
  }

  @Test
  fun theDateWins_whenBothAreSet() {
    val level =
        passWaterLevel(pass(added = 0, expires = 2_000_000, maxEntries = 10), usedEntries = 9, now)

    assertEquals(0.5f, level!!, 0.0001f)
  }

  @Test
  fun passWithNeitherDateNorLimit_hasNoLevel() {
    assertNull(passWaterLevel(pass(), usedEntries = 4, now = now))
  }

  @Test
  fun passWaterLabel_roundsToAWholePercentage() {
    assertEquals("68%", passWaterLabel(0.676f))
    assertEquals("0%", passWaterLabel(0f))
    assertEquals("100%", passWaterLabel(1f))
  }

  @Test
  fun entryCapsules_marksUsedEntriesFirst_thenTheAvailableOnes() {
    assertEquals(
        listOf(false, false, true, true),
        entryCapsules(pass(maxEntries = 4), usedEntries = 2),
    )
  }

  @Test
  fun entryCapsules_neverGoesPastTheLimitOrBelowZero() {
    assertEquals(List(3) { false }, entryCapsules(pass(maxEntries = 3), usedEntries = 9))
    assertEquals(List(3) { true }, entryCapsules(pass(maxEntries = 3), usedEntries = -1))
  }

  @Test
  fun entryCapsules_isEmpty_forADatedOrUnlimitedOrTooBigOrEmptyPass() {
    assertEquals(emptyList<Boolean>(), entryCapsules(pass(expires = 5, maxEntries = 4), 0))
    assertEquals(emptyList<Boolean>(), entryCapsules(pass(), 0))
    assertEquals(emptyList<Boolean>(), entryCapsules(pass(maxEntries = MAX_ENTRY_CAPSULES + 1), 0))
    assertEquals(emptyList<Boolean>(), entryCapsules(pass(maxEntries = 0), 0))
  }
}
