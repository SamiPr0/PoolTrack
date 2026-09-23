package com.github.se.pooltrack.ui.history

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val older = Entry(timestampEpochMilli = 1_000L, subscriptionId = "sub-1")
  private val newer = Entry(timestampEpochMilli = 2_000L, subscriptionId = "sub-1")

  @Test
  fun entries_isEmpty_whenNoEntriesRecorded() {
    val viewModel = HistoryViewModel(FakeEntryRepository())

    assertEquals(emptyList<Entry>(), viewModel.entries.value)
  }

  @Test
  fun entries_listsEveryEntryMostRecentFirst_whenEntriesRecorded() {
    val viewModel = HistoryViewModel(FakeEntryRepository(listOf(older, newer)))

    assertEquals(listOf(newer, older), viewModel.entries.value)
  }

  @Test
  fun entries_updates_whenRepositoryRecordsAnEntry() = runTest {
    val repository = FakeEntryRepository(listOf(older))
    val viewModel = HistoryViewModel(repository)

    repository.addEntry(newer)

    assertEquals(listOf(newer, older), viewModel.entries.value)
  }

  @Test
  fun onDeleteEntry_removesOnlyThatEntry() {
    val repository = FakeEntryRepository(listOf(older, newer))
    val viewModel = HistoryViewModel(repository)

    viewModel.onDeleteEntry(newer)

    assertEquals(listOf(older), repository.storedEntries)
    assertEquals(listOf(older), viewModel.entries.value)
  }
}
