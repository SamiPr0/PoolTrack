package com.github.se.pooltrack.model.backup

import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirestoreBackupTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun mirrorToFirestore_writesDataUnderUserCollection_whenSignedIn() = runTest {
    val firebase = FirebaseMocks(uid = "user-42")
    val data = mapOf("id" to "a", "maxEntries" to null, "price" to 12.5)

    mirrorToFirestore(collection = "subscriptions", docId = "doc-1", data = data)

    verify(exactly = 1) { firebase.firestore.collection("users") }
    verify(exactly = 1) { firebase.usersCollection.document("user-42") }
    verify(exactly = 1) { firebase.userDocument.collection("subscriptions") }
    verify(exactly = 1) { firebase.targetCollection.document("doc-1") }
    verify(exactly = 1) { firebase.targetDocument.set(data) }
  }

  @Test
  fun mirrorToFirestore_doesNothing_whenSignedOut() = runTest {
    val firebase = FirebaseMocks(uid = null)

    mirrorToFirestore(collection = "entries", docId = "doc", data = mapOf("a" to 1))

    verify(exactly = 0) { firebase.firestore.collection(any()) }
  }

  @Test
  fun mirrorToFirestore_swallowsFailure_whenWriteFails() = runTest {
    val firebase = FirebaseMocks()
    firebase.failWrites()

    mirrorToFirestore(collection = "entries", docId = "doc", data = mapOf("a" to 1))

    verify(exactly = 1) { firebase.targetDocument.set(mapOf("a" to 1)) }
  }

  @Test
  fun deleteFromFirestore_deletesDocumentUnderUserCollection_whenSignedIn() = runTest {
    val firebase = FirebaseMocks(uid = "user-42")

    deleteFromFirestore(collection = "entries", docId = "doc-9")

    verify(exactly = 1) { firebase.usersCollection.document("user-42") }
    verify(exactly = 1) { firebase.userDocument.collection("entries") }
    verify(exactly = 1) { firebase.targetCollection.document("doc-9") }
    verify(exactly = 1) { firebase.targetDocument.delete() }
  }

  @Test
  fun deleteFromFirestore_doesNothing_whenSignedOut() = runTest {
    val firebase = FirebaseMocks(uid = null)

    deleteFromFirestore(collection = "entries", docId = "doc")

    verify(exactly = 0) { firebase.firestore.collection(any()) }
  }

  @Test
  fun deleteFromFirestore_swallowsFailure_whenDeleteFails() = runTest {
    val firebase = FirebaseMocks()
    firebase.failWrites()

    deleteFromFirestore(collection = "entries", docId = "doc")

    verify(exactly = 1) { firebase.targetDocument.delete() }
  }

  @Test
  fun awaitResult_returnsValue_whenTaskSucceeds() = runTest {
    assertEquals("done", completedTask("done").awaitResult())
  }

  @Test
  fun awaitResult_throwsTaskException_whenTaskFails() = runTest {
    val error = IllegalStateException("boom")

    try {
      failedTask<String>(error).awaitResult()
      fail("expected the task's exception")
    } catch (thrown: IllegalStateException) {
      // Coroutine stack-trace recovery may copy the exception, so compare its message.
      assertEquals(error.message, thrown.message)
    }
  }
}
