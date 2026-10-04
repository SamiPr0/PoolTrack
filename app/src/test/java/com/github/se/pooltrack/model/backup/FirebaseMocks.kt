package com.github.se.pooltrack.model.backup

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

/**
 * A [Task] that is already successfully completed. Its listeners fire synchronously, so no main
 * looper is needed (the real `Tasks.forResult` posts its callbacks to the main thread).
 */
internal fun <T> completedTask(value: T): Task<T> {
  val task = mockk<Task<T>>()
  every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } answers
      {
        firstArg<OnSuccessListener<T>>().onSuccess(value)
        task
      }
  every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
  return task
}

/** A [Task] that already failed with [error]; see [completedTask]. */
internal fun <T> failedTask(error: Exception): Task<T> {
  val task = mockk<Task<T>>()
  every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } returns task
  every { task.addOnFailureListener(any<OnFailureListener>()) } answers
      {
        firstArg<OnFailureListener>().onFailure(error)
        task
      }
  return task
}

@Suppress("UNCHECKED_CAST")
internal fun voidTask(): Task<Void> = completedTask<Void?>(null) as Task<Void>

/**
 * Stubs `FirebaseAuth.getInstance()` and `FirebaseFirestore.getInstance()` so that nothing touches
 * the network, and records the `users/{uid}/{collection}/{docId}` chain the backup walks.
 *
 * Call `unmockkAll()` in `@After` to undo the static mocks.
 *
 * @param uid The signed-in user's uid, or `null` for a signed-out user.
 */
internal class FirebaseMocks(uid: String? = "uid-1") {
  val user: FirebaseUser? = uid?.let { id ->
    mockk<FirebaseUser>().also { every { it.uid } returns id }
  }
  val auth: FirebaseAuth = mockk()
  val firestore: FirebaseFirestore = mockk()
  val usersCollection: CollectionReference = mockk()
  val userDocument: DocumentReference = mockk()
  val targetCollection: CollectionReference = mockk()
  val targetDocument: DocumentReference = mockk()

  init {
    mockkStatic(FirebaseAuth::class)
    mockkStatic(FirebaseFirestore::class)
    every { FirebaseAuth.getInstance() } returns auth
    every { auth.currentUser } returns user
    every { FirebaseFirestore.getInstance() } returns firestore
    every { firestore.collection(any()) } returns usersCollection
    every { usersCollection.document(any<String>()) } returns userDocument
    every { userDocument.collection(any()) } returns targetCollection
    every { targetCollection.document(any<String>()) } returns targetDocument
    succeedWrites()
  }

  /** Forgets the calls made so far (e.g. during test setup) but keeps the stubbing. */
  fun clearRecordedCalls() {
    clearMocks(
        firestore,
        usersCollection,
        userDocument,
        targetCollection,
        targetDocument,
        answers = false,
    )
  }

  fun succeedWrites() {
    every { targetDocument.set(any<Any>()) } returns voidTask()
    every { targetDocument.delete() } returns voidTask()
  }

  fun failWrites(error: Exception = RuntimeException("offline")) {
    every { targetDocument.set(any<Any>()) } returns failedTask(error)
    every { targetDocument.delete() } returns failedTask(error)
  }
}
