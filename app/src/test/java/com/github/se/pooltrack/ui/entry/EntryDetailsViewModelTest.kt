package com.github.se.pooltrack.ui.entry

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EntryDetailsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val subscription =
      Subscription(
          id = "sub-1",
          uri = "content://pass",
          displayName = "Pool 10x",
          addedAtEpochMilli = 0L,
          maxEntries = 10,
          price = 50.0,
      )
  private val older = Entry(timestampEpochMilli = 1_000L, subscriptionId = "sub-1")
  private val newer = Entry(timestampEpochMilli = 2_000L, subscriptionId = "sub-1")

  private fun viewModel(entries: FakeEntryRepository) =
      EntryDetailsViewModel(entries, FakeSubscriptionRepository(listOf(subscription)))

  @Test
  fun details_isNull_beforeAnEntryIsLoaded() {
    val viewModel = viewModel(FakeEntryRepository(listOf(older, newer)))

    assertNull(viewModel.details.value)
  }

  @Test
  fun loadEntry_exposesThatEntrysDetails() {
    val viewModel = viewModel(FakeEntryRepository(listOf(older, newer)))

    viewModel.loadEntry(newer.timestampEpochMilli)

    assertEquals(
        EntryDetails(
            entry = newer,
            swimDuration = null,
            swimNumber = 2,
            subscription = subscription,
            entryNumberOnSubscription = 2,
            costOfEntry = 5.0,
            daysSincePreviousSwim = 0L,
        ),
        viewModel.details.value,
    )
  }

  @Test
  fun loadEntry_exposesNull_whenNoEntryHasThatTimestamp() {
    val viewModel = viewModel(FakeEntryRepository(listOf(older)))

    viewModel.loadEntry(9_999L)

    assertNull(viewModel.details.value)
  }

  @Test
  fun details_updates_whenTheSwimDurationIsRecordedAfterLoading() = runTest {
    val repository = FakeEntryRepository(listOf(older))
    val viewModel = viewModel(repository)
    viewModel.loadEntry(older.timestampEpochMilli)

    repository.recordSwimDuration(older, durationMillis = 3_600_000L)

    assertEquals(Duration.ofHours(1), viewModel.details.value?.swimDuration)
    assertEquals(older.copy(swimDurationMillis = 3_600_000L), viewModel.details.value?.entry)
  }

  @Test
  fun onDeleteEntry_removesOnlyTheLoadedEntry() {
    val repository = FakeEntryRepository(listOf(older, newer))
    val viewModel = viewModel(repository)
    viewModel.loadEntry(older.timestampEpochMilli)

    viewModel.onDeleteEntry()

    assertEquals(listOf(newer), repository.storedEntries)
    assertNull(viewModel.details.value)
  }

  @Test
  fun onDeleteEntry_doesNothing_whenNoEntryIsLoaded() {
    val repository = FakeEntryRepository(listOf(older, newer))
    val viewModel = viewModel(repository)

    viewModel.onDeleteEntry()

    assertEquals(listOf(older, newer), repository.storedEntries)
  }
}
