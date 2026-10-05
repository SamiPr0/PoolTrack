package com.github.se.pooltrack.model.entry

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.se.pooltrack.model.backup.deleteFromFirestore
import com.github.se.pooltrack.model.backup.mirrorToFirestore
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.entryDataStore by preferencesDataStore(name = "entry_prefs")

// Each account's entries live under their own key, so one account never sees another's.
private fun entriesKey(uid: String) = stringPreferencesKey("entries_json_$uid")

// Entries recorded before they were scoped to an account: first a JSON list, and before that
// plain newline-separated timestamps with no subscription association. The first account to sign
// in afterwards claims them (see claimLegacyEntries), since it's the device's owner.
private val LEGACY_ENTRIES_KEY = stringPreferencesKey("entries_json")
private val LEGACY_ENTRY_TIMESTAMPS_KEY = stringPreferencesKey("entry_timestamps")
private const val LEGACY_TIMESTAMP_SEPARATOR = "\n"

private const val BACKUP_COLLECTION = "entries"

// Entry has no id of its own (entries are deduped structurally, see readEntries/deleteEntry
// below), so this synthesizes a stable-enough Firestore document id from its own fields.
private fun Entry.backupDocId() = "${timestampEpochMilli}_${subscriptionId ?: "none"}"

/**
 * Stores confirmed entries on-device, using Jetpack DataStore, separately for each account.
 *
 * @param currentUid Emits the signed-in account's uid, or `null` when signed out. Reads only ever
 *   see that account's entries (none when signed out), and writes require someone to be signed in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepositoryLocal(
    private val context: Context,
    private val currentUid: Flow<String?>,
) : EntryRepository {

  override fun getEntries(): Flow<List<Entry>> =
      currentUid.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) {
          flowOf(emptyList())
        } else {
          flow {
            context.entryDataStore.edit { claimLegacyEntries(it, uid) }
            emitAll(
                context.entryDataStore.data.map { prefs ->
                  readEntries(prefs, uid).sortedByDescending { it.timestamp }
                }
            )
          }
        }
      }

  override suspend fun addEntry(entry: Entry) {
    val uid = requireUid()
    context.entryDataStore.edit { prefs ->
      claimLegacyEntries(prefs, uid)
      prefs[entriesKey(uid)] = Json.encodeToString(readEntries(prefs, uid) + entry)
    }
    mirrorToFirestore(
        collection = BACKUP_COLLECTION,
        docId = entry.backupDocId(),
        data =
            mapOf(
                "timestampEpochMilli" to entry.timestampEpochMilli,
                "subscriptionId" to entry.subscriptionId,
            ),
    )
  }

  override suspend fun deleteEntry(entry: Entry) {
    val uid = requireUid()
    context.entryDataStore.edit { prefs ->
      claimLegacyEntries(prefs, uid)
      prefs[entriesKey(uid)] = Json.encodeToString(readEntries(prefs, uid).filter { it != entry })
    }
    deleteFromFirestore(collection = BACKUP_COLLECTION, docId = entry.backupDocId())
  }

  private suspend fun requireUid(): String =
      checkNotNull(currentUid.first()) { "Entries can only be changed while signed in" }

  private fun readEntries(prefs: Preferences, uid: String): List<Entry> =
      prefs[entriesKey(uid)]?.let { Json.decodeFromString<List<Entry>>(it) } ?: emptyList()

  /** Moves any entries stored before they were scoped to an account over to [uid]'s. */
  private fun claimLegacyEntries(prefs: MutablePreferences, uid: String) {
    val legacy = readLegacyEntries(prefs) ?: return
    if (prefs[entriesKey(uid)] == null) {
      prefs[entriesKey(uid)] = Json.encodeToString(legacy)
    }
    prefs.remove(LEGACY_ENTRIES_KEY)
    prefs.remove(LEGACY_ENTRY_TIMESTAMPS_KEY)
  }

  /** The unscoped entries, from either legacy format, or `null` if there are none. */
  private fun readLegacyEntries(prefs: Preferences): List<Entry>? {
    prefs[LEGACY_ENTRIES_KEY]?.let {
      return Json.decodeFromString<List<Entry>>(it)
    }
    return prefs[LEGACY_ENTRY_TIMESTAMPS_KEY]?.let { raw ->
      raw.split(LEGACY_TIMESTAMP_SEPARATOR)
          .filter { it.isNotBlank() }
          .map { Entry(timestampEpochMilli = Instant.parse(it).toEpochMilli()) }
    }
  }
}
