package com.github.se.pooltrack.model.subscription

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.se.pooltrack.model.ResolverContext
import com.github.se.pooltrack.model.backup.FirebaseMocks
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.model.productionPreferencesDataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SubscriptionRepositoryLocalTest {

  private val pdfUri = "content://docs/pass.pdf"

  private lateinit var appContext: Context
  private lateinit var resolver: ContentResolver
  private lateinit var firebase: FirebaseMocks
  private lateinit var repository: SubscriptionRepositoryLocal

  @Before
  fun setUp() = runTest {
    appContext = RuntimeEnvironment.getApplication()
    clearPreferencesDataStore(appContext, FILE_CLASS, PROPERTY)
    resolver = mockk(relaxed = true)
    answerDisplayName("pass.pdf")
    firebase = FirebaseMocks(uid = "user-1")
    repository = SubscriptionRepositoryLocal(ResolverContext(appContext, resolver))
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun answerCursor(cursor: Cursor?) {
    every { resolver.query(any(), any(), any(), any(), any()) } returns cursor
  }

  private fun answerDisplayName(name: String?) {
    val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
    cursor.addRow(arrayOf<Any?>(name))
    answerCursor(cursor)
  }

  @Test
  fun getSubscriptions_isEmpty_whenNothingWasAdded() = runTest {
    assertEquals(emptyList<Subscription>(), repository.getSubscriptions().first())
  }

  @Test
  fun getActiveSubscription_isNull_whenNothingWasAdded() = runTest {
    assertNull(repository.getActiveSubscription().first())
  }

  @Test
  fun addSubscription_storesSubscriptionAndMakesItActive() = runTest {
    val before = System.currentTimeMillis()
    val added =
        repository.addSubscription(
            uri = pdfUri,
            expiresAtEpochMilli = 9_000L,
            maxEntries = 10,
            price = 60.0,
        )
    val after = System.currentTimeMillis()

    // The production code reads the system clock, so only its range can be asserted.
    assertTrue(added.addedAtEpochMilli in before..after)
    val expected =
        Subscription(
            id = added.id,
            uri = pdfUri,
            displayName = "pass.pdf",
            addedAtEpochMilli = added.addedAtEpochMilli,
            expiresAtEpochMilli = 9_000L,
            maxEntries = 10,
            price = 60.0,
        )
    assertEquals(expected, added)
    assertEquals(UUID.fromString(added.id).toString(), added.id)
    assertEquals(listOf(expected), repository.getSubscriptions().first())
    assertEquals(expected, repository.getActiveSubscription().first())
  }

  @Test
  fun addSubscription_leavesOptionalFieldsNull_whenNotGiven() = runTest {
    val added = repository.addSubscription(uri = pdfUri)

    assertNull(added.expiresAtEpochMilli)
    assertNull(added.maxEntries)
    assertNull(added.price)
  }

  @Test
  fun addSubscription_generatesDistinctIds_whenAddedTwice() = runTest {
    val first = repository.addSubscription(uri = pdfUri)
    val second = repository.addSubscription(uri = pdfUri)

    assertNotEquals(first.id, second.id)
  }

  @Test
  fun addSubscription_requestsPersistableReadPermission() = runTest {
    repository.addSubscription(uri = pdfUri)

    verify(exactly = 1) {
      resolver.takePersistableUriPermission(
          Uri.parse(pdfUri),
          Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
  }

  @Test
  fun addSubscription_usesGenericName_whenProviderReturnsNoCursor() = runTest {
    answerCursor(null)

    assertEquals("Subscription", repository.addSubscription(uri = pdfUri).displayName)
  }

  @Test
  fun addSubscription_usesGenericName_whenCursorIsEmpty() = runTest {
    answerCursor(MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)))

    assertEquals("Subscription", repository.addSubscription(uri = pdfUri).displayName)
  }

  @Test
  fun addSubscription_usesGenericName_whenCursorHasNoDisplayNameColumn() = runTest {
    val cursor = MatrixCursor(arrayOf(OpenableColumns.SIZE))
    cursor.addRow(arrayOf<Any?>(12L))
    answerCursor(cursor)

    assertEquals("Subscription", repository.addSubscription(uri = pdfUri).displayName)
  }

  @Test
  fun addSubscription_usesGenericName_whenDisplayNameIsNull() = runTest {
    answerDisplayName(null)

    assertEquals("Subscription", repository.addSubscription(uri = pdfUri).displayName)
  }

  @Test
  fun addSubscription_mirrorsFieldsWithoutUriToFirestore_whenSignedIn() = runTest {
    val added =
        repository.addSubscription(
            uri = pdfUri,
            expiresAtEpochMilli = 9_000L,
            maxEntries = null,
            price = 60.0,
        )

    verify(exactly = 1) { firebase.userDocument.collection("subscriptions") }
    verify(exactly = 1) { firebase.targetCollection.document(added.id) }
    verify(exactly = 1) {
      firebase.targetDocument.set(
          mapOf(
              "id" to added.id,
              "displayName" to "pass.pdf",
              "addedAtEpochMilli" to added.addedAtEpochMilli,
              "expiresAtEpochMilli" to 9_000L,
              "maxEntries" to null,
              "price" to 60.0,
          )
      )
    }
  }

  @Test
  fun addSubscription_stillStoresLocally_whenMirroringFails() = runTest {
    firebase.failWrites()

    val added = repository.addSubscription(uri = pdfUri)

    assertEquals(listOf(added), repository.getSubscriptions().first())
  }

  @Test
  fun addSubscription_doesNotMirror_whenSignedOut() = runTest {
    val signedOut = FirebaseMocks(uid = null)
    val signedOutRepository =
        SubscriptionRepositoryLocal(ResolverContext(RuntimeEnvironment.getApplication(), resolver))

    val added = signedOutRepository.addSubscription(uri = pdfUri)

    verify(exactly = 0) { signedOut.firestore.collection(any()) }
    assertEquals(listOf(added), signedOutRepository.getSubscriptions().first())
  }

  @Test
  fun getSubscriptions_sortsMostRecentlyAddedFirst() = runTest {
    val oldest = stored(id = "a", addedAt = 2_000L)
    val newest = stored(id = "b", addedAt = 4_000L)
    val middle = stored(id = "c", addedAt = 3_000L)
    productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).edit {
      it[stringPreferencesKey("subscriptions_json")] =
          Json.encodeToString(listOf(oldest, newest, middle))
    }

    assertEquals(listOf(newest, middle, oldest), repository.getSubscriptions().first())
  }

  private fun stored(id: String, addedAt: Long) =
      Subscription(id = id, uri = pdfUri, displayName = id, addedAtEpochMilli = addedAt)

  @Test
  fun addSubscription_makesNewestTheActiveOne_whenSeveralAdded() = runTest {
    repository.addSubscription(uri = pdfUri)
    val second = repository.addSubscription(uri = pdfUri)

    assertEquals(second, repository.getActiveSubscription().first())
  }

  @Test
  fun setActiveSubscription_switchesActiveSubscription() = runTest {
    val first = repository.addSubscription(uri = pdfUri)
    repository.addSubscription(uri = pdfUri)

    repository.setActiveSubscription(first.id)

    assertEquals(first, repository.getActiveSubscription().first())
  }

  @Test
  fun getActiveSubscription_isNull_whenActiveIdMatchesNothing() = runTest {
    repository.addSubscription(uri = pdfUri)

    repository.setActiveSubscription("unknown")

    assertNull(repository.getActiveSubscription().first())
  }

  @Test
  fun deleteSubscription_removesOnlyThatSubscription() = runTest {
    val first = repository.addSubscription(uri = pdfUri)
    val second = repository.addSubscription(uri = pdfUri)

    repository.deleteSubscription(first.id)

    assertEquals(listOf(second), repository.getSubscriptions().first())
    assertEquals(second, repository.getActiveSubscription().first())
  }

  @Test
  fun deleteSubscription_leavesNoActiveSubscription_whenDeletingTheActiveOne() = runTest {
    val only = repository.addSubscription(uri = pdfUri)

    repository.deleteSubscription(only.id)

    assertEquals(emptyList<Subscription>(), repository.getSubscriptions().first())
    assertNull(repository.getActiveSubscription().first())
  }

  @Test
  fun deleteSubscription_removesMirroredDocument_whenSignedIn() = runTest {
    val added = repository.addSubscription(uri = pdfUri)
    firebase.clearRecordedCalls()

    repository.deleteSubscription(added.id)

    verify(exactly = 1) { firebase.userDocument.collection("subscriptions") }
    verify(exactly = 1) { firebase.targetCollection.document(added.id) }
    verify(exactly = 1) { firebase.targetDocument.delete() }
  }

  @Test
  fun deleteSubscription_isANoOp_whenIdIsUnknown() = runTest {
    val added = repository.addSubscription(uri = pdfUri)

    repository.deleteSubscription("unknown")

    assertEquals(listOf(added), repository.getSubscriptions().first())
    assertEquals(added, repository.getActiveSubscription().first())
  }

  private companion object {
    const val FILE_CLASS =
        "com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocalKt"
    const val PROPERTY = "subscriptionDataStore"
  }
}
