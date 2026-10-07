package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.utils.FakeEntryRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwimReporterTest {

  private val enteredAt = Instant.parse("2026-10-05T10:00:00Z")
  private val entry = Entry(timestampEpochMilli = enteredAt.toEpochMilli(), subscriptionId = "a")
  private val reported = mutableListOf<Duration>()

  private fun reporter(repository: FakeEntryRepository) =
      SwimReporter(repository) { reported += it }

  @Test
  fun onPhoneUnlocked_reportsAndRecordsTheDuration() = runTest {
    val repository = FakeEntryRepository(listOf(entry))

    val result = reporter(repository).onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(72)))

    assertTrue(result)
    assertEquals(listOf(Duration.ofMinutes(72)), reported)
    assertEquals(
        listOf(entry.copy(swimDurationMillis = Duration.ofMinutes(72).toMillis())),
        repository.storedEntries,
    )
  }

  @Test
  fun onPhoneUnlocked_reportsOnlyOncePerSwim() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)

    reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(72)))
    val second = reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(80)))

    assertFalse(second)
    assertEquals(1, reported.size)
  }

  @Test
  fun onPhoneUnlocked_doesNothing_beforeTheMinimumDuration() = runTest {
    val repository = FakeEntryRepository(listOf(entry))

    val result = reporter(repository).onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(3)))

    assertFalse(result)
    assertTrue(reported.isEmpty())
    assertEquals(listOf(entry), repository.storedEntries)
  }

  @Test
  fun onPhoneUnlocked_doesNothing_withoutEntries() = runTest {
    assertFalse(reporter(FakeEntryRepository()).onPhoneUnlocked(enteredAt))
    assertTrue(reported.isEmpty())
  }

  @Test
  fun onPhoneUnlocked_usesTheMostRecentEntry() = runTest {
    val older = entry.copy(timestampEpochMilli = enteredAt.minus(Duration.ofDays(2)).toEpochMilli())
    val repository = FakeEntryRepository(listOf(older, entry))

    reporter(repository).onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(30)))

    assertEquals(listOf(Duration.ofMinutes(30)), reported)
  }

  @Test
  fun hasPendingSwim_reflectsTheMostRecentEntry() = runTest {
    assertFalse(reporter(FakeEntryRepository()).hasPendingSwim(enteredAt))
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    assertTrue(reporter.hasPendingSwim(enteredAt))

    reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(30)))

    assertFalse(reporter.hasPendingSwim(enteredAt.plus(Duration.ofMinutes(31))))
  }

  @Test
  fun onPhoneUnlocked_afterTheEntryWasRemoved_reportsNothing() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking(enteredAt)

    repository.deleteEntry(entry)
    val result = reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(30)))

    assertFalse(result)
    assertTrue(reported.isEmpty())
  }

  @Test
  fun onPhoneUnlocked_neverFallsBackToAnOlderUnreportedEntry() = runTest {
    val older =
        entry.copy(timestampEpochMilli = enteredAt.minus(Duration.ofHours(1)).toEpochMilli())
    val newer = entry.copy(timestampEpochMilli = enteredAt.toEpochMilli(), subscriptionId = "b")
    val repository = FakeEntryRepository(listOf(older, newer))
    val reporter = reporter(repository)
    reporter.startTracking(enteredAt)

    repository.deleteEntry(newer)
    val result = reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(30)))

    assertFalse(result)
    assertTrue(reported.isEmpty())
    assertEquals(listOf(older), repository.storedEntries)
  }

  @Test
  fun startTracking_isFalse_whenTheLatestSwimIsAlreadyReportedOrTooOld() = runTest {
    val reportedEntry = entry.copy(swimDurationMillis = 1L)
    assertFalse(reporter(FakeEntryRepository(listOf(reportedEntry))).startTracking(enteredAt))
    assertFalse(
        reporter(FakeEntryRepository(listOf(entry)))
            .startTracking(enteredAt.plus(Duration.ofDays(1)))
    )
  }

  @Test
  fun hasPendingSwim_isFalse_onceTheTrackedEntryIsRemoved() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking(enteredAt)

    repository.deleteEntry(entry)

    assertFalse(reporter.hasPendingSwim(enteredAt))
  }

  @Test
  fun awaitTrackedEntryRemoved_returnsOnceTheEntryIsDeleted() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking(enteredAt)
    var returned = false
    val job = launch {
      reporter.awaitTrackedEntryRemoved()
      returned = true
    }
    runCurrent()
    assertFalse(returned)

    repository.deleteEntry(entry)
    runCurrent()

    assertTrue(returned)
    job.cancel()
  }
}
