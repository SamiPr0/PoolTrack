package com.github.se.pooltrack.model.entry

import com.github.se.pooltrack.utils.FakeEntryRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests the behaviour [EntryRepository] provides by default, i.e. to every implementation. */
class EntryRepositoryTest {

  @Test
  fun timestamp_isTheConfirmationInstant() {
    assertEquals(Instant.ofEpochMilli(1_234L), Entry(timestampEpochMilli = 1_234L).timestamp)
  }

  @Test
  fun getLastEntryTimestamp_isTheMostRecentEntry_whenEntriesRecorded() = runTest {
    val repository =
        FakeEntryRepository(
            listOf(
                Entry(timestampEpochMilli = 2_000L),
                Entry(timestampEpochMilli = 3_000L),
                Entry(timestampEpochMilli = 1_000L),
            )
        )

    assertEquals(Instant.ofEpochMilli(3_000L), repository.getLastEntryTimestamp().first())
  }

  @Test
  fun deleteEntry_matchesTimestampAndSubscription_ignoringSwimDuration() = runTest {
    val stored = Entry(timestampEpochMilli = 1_000L, subscriptionId = "a", swimDurationMillis = 5L)
    val other = Entry(timestampEpochMilli = 1_000L, subscriptionId = "b")
    val repository = FakeEntryRepository(listOf(stored, other))

    repository.deleteEntry(stored.copy(swimDurationMillis = null))

    assertEquals(listOf(other), repository.storedEntries)
  }

  @Test
  fun getLastEntryTimestamp_isNull_whenNoEntriesRecorded() = runTest {
    assertNull(FakeEntryRepository().getLastEntryTimestamp().first())
  }

  @Test
  fun getLastEntryTimestamp_movesBack_whenTheLatestEntryIsDeleted() = runTest {
    val latest = Entry(timestampEpochMilli = 3_000L)
    val repository = FakeEntryRepository(listOf(Entry(timestampEpochMilli = 1_000L), latest))

    repository.deleteEntry(latest)

    assertEquals(Instant.ofEpochMilli(1_000L), repository.getLastEntryTimestamp().first())
  }

  @Test
  fun getLastEntryTimestamp_isTheMostRecentEntry_whenCalledThroughLegacyDefaultImpls() = runTest {
    // Kotlin keeps a static `DefaultImpls` copy of every interface default for consumers
    // compiled without Java default methods; call it the way such a consumer would.
    val repository = FakeEntryRepository(listOf(Entry(1_000L), Entry(3_000L)))
    val defaultImpls = Class.forName("${EntryRepository::class.java.name}\$DefaultImpls")

    @Suppress("UNCHECKED_CAST")
    val flow =
        defaultImpls
            .getMethod("getLastEntryTimestamp", EntryRepository::class.java)
            .invoke(null, repository) as Flow<Instant?>

    assertEquals(Instant.ofEpochMilli(3_000L), flow.first())
  }
}
