package com.github.se.pooltrack.model.entry

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.entryDataStore by preferencesDataStore(name = "entry_prefs")

private val ENTRIES_KEY = stringPreferencesKey("entries_json")

// Read once entries move to JSON storage below, to migrate whatever was recorded before that -
// plain newline-separated timestamps, with no subscription association.
private val LEGACY_ENTRY_TIMESTAMPS_KEY = stringPreferencesKey("entry_timestamps")
private const val LEGACY_TIMESTAMP_SEPARATOR = "\n"

/** Stores confirmed entries on-device, using Jetpack DataStore. */
class EntryRepositoryLocal(private val context: Context) : EntryRepository {

  override fun getEntries(): Flow<List<Entry>> =
      context.entryDataStore.data.map { prefs ->
        readEntries(prefs).sortedByDescending { it.timestamp }
      }

  override suspend fun addEntry(entry: Entry) {
    context.entryDataStore.edit { prefs ->
      val existing = readEntries(prefs)
      prefs[ENTRIES_KEY] = Json.encodeToString(existing + entry)
      prefs.remove(LEGACY_ENTRY_TIMESTAMPS_KEY)
    }
  }

  override suspend fun deleteEntry(entry: Entry) {
    context.entryDataStore.edit { prefs ->
      val existing = readEntries(prefs)
      prefs[ENTRIES_KEY] = Json.encodeToString(existing.filter { it != entry })
      prefs.remove(LEGACY_ENTRY_TIMESTAMPS_KEY)
    }
  }

  /** Reads from the current JSON storage, falling back to the legacy plain-text format. */
  private fun readEntries(prefs: Preferences): List<Entry> {
    prefs[ENTRIES_KEY]?.let { return Json.decodeFromString<List<Entry>>(it) }
    return prefs[LEGACY_ENTRY_TIMESTAMPS_KEY]?.let { raw ->
      raw.split(LEGACY_TIMESTAMP_SEPARATOR)
          .filter { it.isNotBlank() }
          .map { Entry(timestampEpochMilli = Instant.parse(it).toEpochMilli()) }
    } ?: emptyList()
  }
}
