package com.github.se.pooltrack.model.entry

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.entryDataStore by preferencesDataStore(name = "entry_prefs")

private val ENTRY_TIMESTAMPS_KEY = stringPreferencesKey("entry_timestamps")
private const val TIMESTAMP_SEPARATOR = "\n"

/** Stores confirmed entries on-device, using Jetpack DataStore. */
class EntryRepositoryLocal(private val context: Context) : EntryRepository {

  override fun getEntries(): Flow<List<Entry>> =
      context.entryDataStore.data.map { prefs ->
        val raw = prefs[ENTRY_TIMESTAMPS_KEY] ?: return@map emptyList()
        raw.split(TIMESTAMP_SEPARATOR)
            .filter { it.isNotBlank() }
            .map { Entry(Instant.parse(it)) }
            .sortedByDescending { it.timestamp }
      }

  override suspend fun addEntry(entry: Entry) {
    context.entryDataStore.edit { prefs ->
      val existing = prefs[ENTRY_TIMESTAMPS_KEY]
      val newTimestamp = entry.timestamp.toString()
      prefs[ENTRY_TIMESTAMPS_KEY] =
          if (existing.isNullOrEmpty()) newTimestamp
          else existing + TIMESTAMP_SEPARATOR + newTimestamp
    }
  }

  override suspend fun deleteEntry(entry: Entry) {
    context.entryDataStore.edit { prefs ->
      val existing = prefs[ENTRY_TIMESTAMPS_KEY] ?: return@edit
      prefs[ENTRY_TIMESTAMPS_KEY] =
          existing
              .split(TIMESTAMP_SEPARATOR)
              .filter { it.isNotBlank() && it != entry.timestamp.toString() }
              .joinToString(TIMESTAMP_SEPARATOR)
    }
  }
}
