package com.github.se.pooltrack.model.entry

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.se.pooltrack.model.backup.FirebaseMocks
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.model.productionPreferencesDataStore
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EntryRepositoryLocalTest {

  private val oldest = Entry(timestampEpochMilli = 1_000L, subscriptionId = "pass-a")
  private val middle = Entry(timestampEpochMilli = 2_000L, subscriptionId = null)
  private val newest = Entry(timestampEpochMilli = 3_000L, subscriptionId = "pass-b")

  private val currentUid = MutableStateFlow<String?>("user-1")

  private lateinit var appContext: Context
  private lateinit var firebase: FirebaseMocks
  private lateinit var repository: EntryRepositoryLocal

  @Before
  fun setUp() = runTest {
    appContext = RuntimeEnvironment.getApplication()
    clearPreferencesDataStore(appContext, FILE_CLASS, PROPERTY)
    firebase = FirebaseMocks(uid = "user-1")
    repository = EntryRepositoryLocal(appContext, currentUid)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun getEntries_isEmpty_whenNothingWasRecorded() = runTest {
    assertEquals(emptyList<Entry>(), repository.getEntries().first())
  }

  @Test
  fun getLastEntryTimestamp_isNull_whenNothingWasRecorded() = runTest {
    assertNull(repository.getLastEntryTimestamp().first())
  }

  @Test
  fun addEntry_storesEntry_withItsSubscription() = runTest {
    repository.addEntry(oldest)

    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun getEntries_sortsMostRecentFirst() = runTest {
    repository.addEntry(middle)
    repository.addEntry(newest)
    repository.addEntry(oldest)

    assertEquals(listOf(newest, middle, oldest), repository.getEntries().first())
  }

  @Test
  fun getLastEntryTimestamp_isTheNewestEntry_whenSeveralRecorded() = runTest {
    repository.addEntry(middle)
    repository.addEntry(newest)
    repository.addEntry(oldest)

    assertEquals(Instant.ofEpochMilli(3_000L), repository.getLastEntryTimestamp().first())
  }

  @Test
  fun addEntry_mirrorsEntryToFirestore_whenSignedIn() = runTest {
    repository.addEntry(oldest)

    verify(exactly = 1) { firebase.userDocument.collection("entries") }
    verify(exactly = 1) { firebase.targetCollection.document("1000_pass-a") }
    verify(exactly = 1) {
      firebase.targetDocument.set(
          mapOf(
              "timestampEpochMilli" to 1_000L,
              "subscriptionId" to "pass-a",
              "swimDurationMillis" to null,
          )
      )
    }
  }

  @Test
  fun addEntry_usesNoneInDocId_whenEntryHasNoSubscription() = runTest {
    repository.addEntry(middle)

    verify(exactly = 1) { firebase.targetCollection.document("2000_none") }
    verify(exactly = 1) {
      firebase.targetDocument.set(
          mapOf(
              "timestampEpochMilli" to 2_000L,
              "subscriptionId" to null,
              "swimDurationMillis" to null,
          )
      )
    }
  }

  @Test
  fun addEntry_stillStoresLocally_whenMirroringFails() = runTest {
    firebase.failWrites()

    repository.addEntry(oldest)

    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun addEntry_doesNotMirror_whenSignedOut() = runTest {
    val signedOut = FirebaseMocks(uid = null)

    repository.addEntry(oldest)

    verify(exactly = 0) { signedOut.firestore.collection(any()) }
    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun recordSwimDuration_storesDurationOnThatEntryOnly() = runTest {
    repository.addEntry(oldest)
    repository.addEntry(newest)

    repository.recordSwimDuration(newest, 4_000L)

    assertEquals(
        listOf(newest.copy(swimDurationMillis = 4_000L), oldest),
        repository.getEntries().first(),
    )
  }

  @Test
  fun recordSwimDuration_mirrorsUpdatedEntryToFirestore() = runTest {
    repository.addEntry(oldest)

    repository.recordSwimDuration(oldest, 4_000L)

    verify(exactly = 1) {
      firebase.targetDocument.set(
          mapOf(
              "timestampEpochMilli" to 1_000L,
              "subscriptionId" to "pass-a",
              "swimDurationMillis" to 4_000L,
          )
      )
    }
  }

  @Test
  fun recordSwimDuration_onlyTouchesTheSignedInAccount() = runTest {
    repository.addEntry(oldest)
    currentUid.value = "user-2"
    repository.addEntry(oldest)

    repository.recordSwimDuration(oldest, 4_000L)

    currentUid.value = "user-1"
    assertEquals(listOf(oldest), repository.getEntries().first())
    currentUid.value = "user-2"
    assertEquals(
        listOf(oldest.copy(swimDurationMillis = 4_000L)),
        repository.getEntries().first(),
    )
  }

  @Test
  fun recordSwimDuration_doesNothing_whenEntryWasDeleted() = runTest {
    repository.addEntry(oldest)
    repository.deleteEntry(oldest)

    repository.recordSwimDuration(oldest, 4_000L)

    assertEquals(emptyList<Entry>(), repository.getEntries().first())
  }

  @Test
  fun deleteEntry_removesOnlyThatEntry() = runTest {
    repository.addEntry(oldest)
    repository.addEntry(middle)
    repository.addEntry(newest)

    repository.deleteEntry(middle)

    assertEquals(listOf(newest, oldest), repository.getEntries().first())
  }

  @Test
  fun deleteEntry_keepsEntryWithSameTimestampButOtherSubscription() = runTest {
    val sameTimeOtherPass = oldest.copy(subscriptionId = "pass-b")
    repository.addEntry(oldest)
    repository.addEntry(sameTimeOtherPass)

    repository.deleteEntry(oldest)

    assertEquals(listOf(sameTimeOtherPass), repository.getEntries().first())
  }

  @Test
  fun deleteEntry_removesTheEntry_evenFromAStaleSnapshotWithoutSwimDuration() = runTest {
    repository.addEntry(oldest)
    repository.recordSwimDuration(oldest, 60_000L)

    repository.deleteEntry(oldest)

    assertEquals(emptyList<Entry>(), repository.getEntries().first())
  }

  @Test
  fun deleteEntry_removesMirroredDocument_whenSignedIn() = runTest {
    repository.addEntry(oldest)
    firebase.clearRecordedCalls()

    repository.deleteEntry(oldest)

    verify(exactly = 1) { firebase.userDocument.collection("entries") }
    verify(exactly = 1) { firebase.targetCollection.document("1000_pass-a") }
    verify(exactly = 1) { firebase.targetDocument.delete() }
  }

  @Test
  fun deleteEntry_isANoOp_whenEntryWasNeverRecorded() = runTest {
    repository.addEntry(oldest)

    repository.deleteEntry(newest)

    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun getEntries_onlyShowsTheSignedInAccountsEntries() = runTest {
    repository.addEntry(oldest)

    currentUid.value = "user-2"
    assertEquals(emptyList<Entry>(), repository.getEntries().first())
    repository.addEntry(newest)
    assertEquals(listOf(newest), repository.getEntries().first())

    currentUid.value = "user-1"
    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun deleteEntry_leavesOtherAccountsEntriesUntouched() = runTest {
    repository.addEntry(oldest)
    currentUid.value = "user-2"
    repository.addEntry(oldest)

    repository.deleteEntry(oldest)

    assertEquals(emptyList<Entry>(), repository.getEntries().first())
    currentUid.value = "user-1"
    assertEquals(listOf(oldest), repository.getEntries().first())
  }

  @Test
  fun getEntries_isEmpty_whenSignedOut() = runTest {
    repository.addEntry(oldest)

    currentUid.value = null

    assertEquals(emptyList<Entry>(), repository.getEntries().first())
  }

  @Test
  fun addEntry_fails_whenSignedOut() = runTest {
    currentUid.value = null

    val error = runCatching { repository.addEntry(oldest) }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
  }

  @Test
  fun deleteEntry_fails_whenSignedOut() = runTest {
    currentUid.value = null

    val error = runCatching { repository.deleteEntry(oldest) }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
  }

  @Test
  fun getEntries_claimsLegacyJsonEntries_forFirstAccountOnly() = runTest {
    seedLegacy(LEGACY_JSON_KEY, Json.encodeToString(listOf(oldest, newest)))

    assertEquals(listOf(newest, oldest), repository.getEntries().first())

    currentUid.value = "user-2"
    assertEquals(emptyList<Entry>(), repository.getEntries().first())
    val prefs = productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).data.first()
    assertNull(prefs[LEGACY_JSON_KEY])
  }

  @Test
  fun getEntries_claimsLegacyTimestamps_withoutSubscription() = runTest {
    seedLegacy(LEGACY_TIMESTAMPS_KEY, "2026-09-01T10:00:00Z\n\n2026-09-02T11:30:00Z\n")

    assertEquals(
        listOf(
            Entry(timestampEpochMilli = Instant.parse("2026-09-02T11:30:00Z").toEpochMilli()),
            Entry(timestampEpochMilli = Instant.parse("2026-09-01T10:00:00Z").toEpochMilli()),
        ),
        repository.getEntries().first(),
    )
    val prefs = productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).data.first()
    assertNull(prefs[LEGACY_TIMESTAMPS_KEY])
  }

  @Test
  fun addEntry_keepsLegacyEntries_whenClaimingThem() = runTest {
    seedLegacy(LEGACY_TIMESTAMPS_KEY, "2026-09-01T10:00:00Z")
    val legacy = Entry(timestampEpochMilli = Instant.parse("2026-09-01T10:00:00Z").toEpochMilli())

    repository.addEntry(newest)

    assertEquals(
        listOf(legacy, newest).sortedByDescending { it.timestampEpochMilli },
        repository.getEntries().first(),
    )
  }

  @Test
  fun deleteEntry_claimsLegacyEntriesBeforeDeleting() = runTest {
    seedLegacy(LEGACY_TIMESTAMPS_KEY, "2026-09-01T10:00:00Z")
    val legacy = Entry(timestampEpochMilli = Instant.parse("2026-09-01T10:00:00Z").toEpochMilli())

    repository.deleteEntry(legacy)

    assertEquals(emptyList<Entry>(), repository.getEntries().first())
    val prefs = productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).data.first()
    assertNull(prefs[LEGACY_TIMESTAMPS_KEY])
  }

  @Test
  fun getEntries_dropsLegacyEntries_whenAccountAlreadyHasItsOwn() = runTest {
    repository.addEntry(newest)
    seedLegacy(LEGACY_JSON_KEY, Json.encodeToString(listOf(oldest)))

    assertEquals(listOf(newest), repository.getEntries().first())
    val prefs = productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).data.first()
    assertNull(prefs[LEGACY_JSON_KEY])
  }

  private suspend fun seedLegacy(key: Preferences.Key<String>, raw: String) {
    productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).edit { it[key] = raw }
  }

  private companion object {
    const val FILE_CLASS = "com.github.se.pooltrack.model.entry.EntryRepositoryLocalKt"
    const val PROPERTY = "entryDataStore"
    val LEGACY_JSON_KEY = stringPreferencesKey("entries_json")
    val LEGACY_TIMESTAMPS_KEY = stringPreferencesKey("entry_timestamps")
  }
}
