package com.github.se.pooltrack.model.entry

import kotlinx.coroutines.flow.Flow

/** Represents a repository that logs confirmed pool entries. */
interface EntryRepository {

  /** Emits every confirmed entry recorded so far, most recent first. */
  fun getEntries(): Flow<List<Entry>>

  /**
   * Records a new confirmed entry.
   *
   * @param entry The entry to record.
   */
  suspend fun addEntry(entry: Entry)

  /**
   * Removes a recorded entry, e.g. to undo a misclick.
   *
   * @param entry The entry to remove.
   */
  suspend fun deleteEntry(entry: Entry)
}
