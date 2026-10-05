package com.github.se.pooltrack.ui.entry

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.subscription.Subscription
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryDetailsTest {

  private val zone = ZoneId.of("Europe/Zurich")

  private fun at(day: Int, hour: Int = 18) =
      LocalDateTime.of(2026, 10, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

  private val tenEntries =
      Subscription(
          id = "sub-10x",
          uri = "content://10x",
          displayName = "Pool 10x",
          addedAtEpochMilli = 0L,
          maxEntries = 10,
          price = 50.0,
      )
  private val yearly =
      Subscription(
          id = "sub-year",
          uri = "content://year",
          displayName = "Yearly pass",
          addedAtEpochMilli = 0L,
          price = 400.0,
      )

  private val first = Entry(at(1), subscriptionId = "sub-10x", swimDurationMillis = 3_000_000L)
  private val second = Entry(at(4), subscriptionId = "sub-year")
  private val third = Entry(at(4, hour = 23), subscriptionId = "sub-10x")

  // Most recent first, as the repository emits them.
  private val entries = listOf(third, second, first)

  @Test
  fun computeEntryDetails_describesTheFirstEntry() {
    val details = computeEntryDetails(first.timestampEpochMilli, entries, listOf(tenEntries), zone)

    assertEquals(
        EntryDetails(
            entry = first,
            swimDuration = Duration.ofMinutes(50),
            swimNumber = 1,
            subscription = tenEntries,
            entryNumberOnSubscription = 1,
            costOfEntry = 5.0,
            daysSincePreviousSwim = null,
        ),
        details,
    )
  }

  @Test
  fun computeEntryDetails_countsOnlyEntriesOfTheSameSubscription_andDaysSincePrevious() {
    val details =
        computeEntryDetails(third.timestampEpochMilli, entries, listOf(tenEntries, yearly), zone)

    assertEquals(
        EntryDetails(
            entry = third,
            swimDuration = null,
            swimNumber = 3,
            subscription = tenEntries,
            entryNumberOnSubscription = 2,
            costOfEntry = 5.0,
            daysSincePreviousSwim = 0L,
        ),
        details,
    )
  }

  @Test
  fun computeEntryDetails_hasNoCost_whenSubscriptionIsNotEntryLimited() {
    val details =
        computeEntryDetails(second.timestampEpochMilli, entries, listOf(tenEntries, yearly), zone)

    assertEquals(
        EntryDetails(
            entry = second,
            swimDuration = null,
            swimNumber = 2,
            subscription = yearly,
            entryNumberOnSubscription = 1,
            costOfEntry = null,
            daysSincePreviousSwim = 3L,
        ),
        details,
    )
  }

  @Test
  fun computeEntryDetails_hasNoSubscription_whenItWasDeletedOrNeverSet() {
    val orphan = Entry(at(2), subscriptionId = null)

    val details = computeEntryDetails(orphan.timestampEpochMilli, listOf(orphan), emptyList(), zone)

    assertEquals(
        EntryDetails(
            entry = orphan,
            swimDuration = null,
            swimNumber = 1,
            subscription = null,
            entryNumberOnSubscription = null,
            costOfEntry = null,
            daysSincePreviousSwim = null,
        ),
        details,
    )
  }

  @Test
  fun computeEntryDetails_isNull_whenNoEntryHasThatTimestamp() {
    assertNull(computeEntryDetails(at(9), entries, listOf(tenEntries), zone))
    assertNull(computeEntryDetails(at(9), emptyList(), emptyList(), zone))
  }
}
