package com.github.se.pooltrack.utils

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.isSameEntryAs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [EntryRepository] for unit tests. Emits entries most recent first, like the real one.
 */
class FakeEntryRepository(initialEntries: List<Entry> = emptyList()) : EntryRepository {

  private val entries = MutableStateFlow(initialEntries)

  /** Every stored entry, in insertion order. */
  val storedEntries: List<Entry>
    get() = entries.value

  override fun getEntries(): Flow<List<Entry>> = entries.map { list ->
    list.sortedByDescending { it.timestampEpochMilli }
  }

  override suspend fun addEntry(entry: Entry) {
    entries.value = entries.value + entry
  }

  override suspend fun deleteEntry(entry: Entry) {
    entries.value = entries.value.filterNot { it.isSameEntryAs(entry) }
  }

  override suspend fun recordSwimDuration(entry: Entry, durationMillis: Long) {
    entries.value =
        entries.value.map { if (it == entry) it.copy(swimDurationMillis = durationMillis) else it }
  }
}
