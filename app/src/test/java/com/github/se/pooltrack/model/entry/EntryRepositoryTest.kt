package com.github.se.pooltrack.model.entry

import com.github.se.pooltrack.utils.FakeEntryRepository
import java.time.Instant
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
}
