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
  private val entry =
      Entry(
          timestampEpochMilli = enteredAt.toEpochMilli(),
          subscriptionId = "a",
          awaitingDistance = true,
      )
  private val afterWindow = enteredAt.plus(Duration.ofMinutes(30))
  private var reminders = 0

  private fun reporter(repository: FakeEntryRepository) = SwimReporter(repository) { reminders++ }

  @Test
  fun onPhoneUnlocked_remindsInsteadOfRecordingADuration() = runTest {
    val repository = FakeEntryRepository(listOf(entry))

    val result = reporter(repository).onPhoneUnlocked(afterWindow)

    assertTrue(result)
    assertEquals(1, reminders)
    assertEquals(listOf(entry), repository.storedEntries)
  }

  @Test
  fun onPhoneUnlocked_remindsOnlyOnce() = runTest {
    val reporter = reporter(FakeEntryRepository(listOf(entry)))

    reporter.onPhoneUnlocked(afterWindow)
    val second = reporter.onPhoneUnlocked(afterWindow.plusSeconds(60))

    assertFalse(second)
    assertEquals(1, reminders)
  }

  @Test
  fun onPhoneUnlocked_doesNothing_insideTheCancelWindow() = runTest {
    val reporter = reporter(FakeEntryRepository(listOf(entry)))

    val result = reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofMinutes(9)))

    assertFalse(result)
    assertEquals(0, reminders)
  }

  @Test
  fun onPhoneUnlocked_stillRemindsOnAnEntryManyHoursOld() = runTest {
    val reporter = reporter(FakeEntryRepository(listOf(entry)))

    assertTrue(reporter.onPhoneUnlocked(enteredAt.plus(Duration.ofHours(20))))
  }

  @Test
  fun onPhoneUnlocked_doesNothing_withoutEntries() = runTest {
    assertFalse(reporter(FakeEntryRepository()).onPhoneUnlocked(afterWindow))
    assertEquals(0, reminders)
  }

  @Test
  fun onPhoneUnlocked_doesNothing_forAnEntryNotAwaitingADistance() = runTest {
    val entries =
        listOf(
            entry.copy(awaitingDistance = false), // added by hand, imported or from before
            entry.copy(awaitingDistance = false, swimDistanceMeters = 500),
        )
    for (stored in entries) {
      assertFalse(reporter(FakeEntryRepository(listOf(stored))).onPhoneUnlocked(afterWindow))
    }
    assertEquals(0, reminders)
  }

  @Test
  fun onPhoneUnlocked_afterTheDistanceWasLogged_doesNothing() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking()

    repository.recordSwimDistance(entry, 900)

    assertFalse(reporter.onPhoneUnlocked(afterWindow))
    assertEquals(0, reminders)
  }

  @Test
  fun onPhoneUnlocked_afterTheEntryWasCancelled_remindsAboutNothing() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking()

    repository.deleteEntry(entry)

    assertFalse(reporter.onPhoneUnlocked(afterWindow))
    assertEquals(0, reminders)
  }

  @Test
  fun onPhoneUnlocked_neverFallsBackToAnOlderPendingEntry() = runTest {
    val older =
        entry.copy(timestampEpochMilli = enteredAt.minus(Duration.ofHours(1)).toEpochMilli())
    val newer = entry.copy(subscriptionId = "b")
    val repository = FakeEntryRepository(listOf(older, newer))
    val reporter = reporter(repository)
    reporter.startTracking()

    repository.deleteEntry(newer)

    assertFalse(reporter.onPhoneUnlocked(afterWindow))
    assertEquals(0, reminders)
    assertEquals(listOf(older), repository.storedEntries)
  }

  @Test
  fun startTracking_isTrue_onlyWhenTheLatestEntryAwaitsADistance() = runTest {
    assertTrue(reporter(FakeEntryRepository(listOf(entry))).startTracking())
    assertFalse(reporter(FakeEntryRepository()).startTracking())
    assertFalse(
        reporter(FakeEntryRepository(listOf(entry.copy(awaitingDistance = false)))).startTracking()
    )
  }

  @Test
  fun hasPendingSwim_isFalse_onceReminded() = runTest {
    val reporter = reporter(FakeEntryRepository(listOf(entry)))
    assertTrue(reporter.hasPendingSwim())

    reporter.onPhoneUnlocked(afterWindow)

    assertFalse(reporter.hasPendingSwim())
  }

  @Test
  fun hasPendingSwim_isFalse_onceTheDistanceIsLoggedOrTheEntryCancelled() = runTest {
    val logged = FakeEntryRepository(listOf(entry))
    val loggedReporter = reporter(logged)
    loggedReporter.startTracking()
    logged.recordSwimDistance(entry, 600)
    assertFalse(loggedReporter.hasPendingSwim())

    val cancelled = FakeEntryRepository(listOf(entry))
    val cancelledReporter = reporter(cancelled)
    cancelledReporter.startTracking()
    cancelled.deleteEntry(entry)
    assertFalse(cancelledReporter.hasPendingSwim())
  }

  @Test
  fun awaitTrackedEntryResolved_returnsOnceTheEntryIsDeleted() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking()
    var returned = false
    val job = launch {
      reporter.awaitTrackedEntryResolved()
      returned = true
    }
    runCurrent()
    assertFalse(returned)

    repository.deleteEntry(entry)
    runCurrent()

    assertTrue(returned)
    job.cancel()
  }

  @Test
  fun awaitTrackedEntryResolved_returnsOnceTheDistanceIsLogged() = runTest {
    val repository = FakeEntryRepository(listOf(entry))
    val reporter = reporter(repository)
    reporter.startTracking()
    var returned = false
    val job = launch {
      reporter.awaitTrackedEntryResolved()
      returned = true
    }
    runCurrent()
    assertFalse(returned)

    repository.recordSwimDistance(entry, 700)
    runCurrent()

    assertTrue(returned)
    job.cancel()
  }

  @Test
  fun awaitTrackedEntryResolved_returnsAtOnce_whenNothingIsTracked() = runTest {
    reporter(FakeEntryRepository()).awaitTrackedEntryResolved()
  }
}
