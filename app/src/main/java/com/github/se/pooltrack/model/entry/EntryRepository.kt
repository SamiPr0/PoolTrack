package com.github.se.pooltrack.model.entry

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

  /**
   * Stores how long the swim of an already recorded entry lasted. Does nothing if [entry] is no
   * longer recorded (e.g. it was deleted in the meantime).
   *
   * @param entry The entry the swim belongs to.
   * @param durationMillis The swim duration, in milliseconds.
   */
  suspend fun recordSwimDuration(entry: Entry, durationMillis: Long)

  /**
   * Emits the timestamp of the most recent entry, or `null` if there is none. This is the single
   * source of truth for "when did I last enter the pool" - derived from the entries themselves so
   * that deleting the latest one (e.g. to undo a misclick) is immediately reflected in it.
   */
  fun getLastEntryTimestamp(): Flow<Instant?> =
      getEntries().map { entries -> entries.maxOfOrNull { it.timestamp } }
}
