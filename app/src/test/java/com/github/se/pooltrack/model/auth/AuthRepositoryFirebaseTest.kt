package com.github.se.pooltrack.model.auth

import android.content.Context
import android.content.res.Resources
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.github.se.pooltrack.model.backup.FirebaseMocks
import com.github.se.pooltrack.model.backup.completedTask
import com.github.se.pooltrack.model.backup.failedTask
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthRepositoryFirebaseTest {

  private val googleUser = AuthUser("uid-g", "Sami", "sami@example.com", isAnonymous = false)
  private val anonymousUser = AuthUser("uid-a", null, null, isAnonymous = true)

  private lateinit var firebase: FirebaseMocks
  private lateinit var repository: AuthRepositoryFirebase

  @Before
  fun setUp() {
    firebase = FirebaseMocks(uid = null)
    repository = AuthRepositoryFirebase()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun firebaseUser(user: AuthUser): FirebaseUser = mockk {
    every { uid } returns user.uid
    every { displayName } returns user.displayName
    every { email } returns user.email
    every { isAnonymous } returns user.isAnonymous
  }

  private fun authResult(user: FirebaseUser?): AuthResult = mockk {
    every { this@mockk.user } returns user
  }

  /** A context whose resources do (or, for a null [webClientId], do not) hold the web client id. */
  private fun context(webClientId: String?): Context {
    val resources: Resources = mockk()
    every { resources.getIdentifier("default_web_client_id", "string", "pkg") } returns
        if (webClientId == null) 0 else 7
    return mockk {
      every { this@mockk.resources } returns resources
      every { packageName } returns "pkg"
      if (webClientId != null) every { getString(7) } returns webClientId
    }
  }

  /** Stubs the system account picker to hand back a Google ID token, or to throw [error]. */
  private fun pickAccount(error: Exception? = null): CredentialManager {
    val manager: CredentialManager = mockk()
    mockkObject(CredentialManager.Companion)
    every { CredentialManager.create(any()) } returns manager
    if (error != null) {
      coEvery { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws error
    } else {
      val credential =
          GoogleIdTokenCredential(
              id = "sami@example.com",
              idToken = "token-123",
              displayName = null,
              familyName = null,
              givenName = null,
              profilePictureUri = null,
              phoneNumber = null,
          )
      coEvery { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
          GetCredentialResponse(credential)
    }
    return manager
  }

  private fun stubFirebaseCredential(): AuthCredential {
    val firebaseCredential: AuthCredential = mockk()
    mockkStatic(GoogleAuthProvider::class)
    every { GoogleAuthProvider.getCredential("token-123", null) } returns firebaseCredential
    return firebaseCredential
  }

  @Test
  fun getCurrentUser_emitsMappedUserAndNull_whenAuthStateChanges() = runTest {
    val listener = slot<FirebaseAuth.AuthStateListener>()
    every { firebase.auth.addAuthStateListener(capture(listener)) } returns Unit
    every { firebase.auth.removeAuthStateListener(any()) } returns Unit
    val emitted = mutableListOf<AuthUser?>()

    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          repository.getCurrentUser().toList(emitted)
        }
    val signedIn: FirebaseAuth = mockk { every { currentUser } returns firebaseUser(googleUser) }
    val signedOut: FirebaseAuth = mockk { every { currentUser } returns null }
    listener.captured.onAuthStateChanged(signedIn)
    listener.captured.onAuthStateChanged(signedOut)
    job.cancelAndJoin()

    assertEquals(listOf<AuthUser?>(googleUser, null), emitted)
  }

  // The app no longer starts anonymous sessions, but one saved by an older debug build must still
  // be reported as anonymous, since that is what keeps it on the sign-in screen.
  @Test
  fun getCurrentUser_reportsRestoredAnonymousSession_asAnonymous() = runTest {
    val listener = slot<FirebaseAuth.AuthStateListener>()
    every { firebase.auth.addAuthStateListener(capture(listener)) } returns Unit
    every { firebase.auth.removeAuthStateListener(any()) } returns Unit
    val emitted = mutableListOf<AuthUser?>()

    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          repository.getCurrentUser().toList(emitted)
        }
    val restored: FirebaseAuth = mockk { every { currentUser } returns firebaseUser(anonymousUser) }
    listener.captured.onAuthStateChanged(restored)
    job.cancelAndJoin()

    assertEquals(listOf<AuthUser?>(anonymousUser), emitted)
  }

  @Test
  fun getCurrentUser_removesListener_whenCollectionIsCancelled() = runTest {
    val listener = slot<FirebaseAuth.AuthStateListener>()
    every { firebase.auth.addAuthStateListener(capture(listener)) } returns Unit
    every { firebase.auth.removeAuthStateListener(any()) } returns Unit

    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          repository.getCurrentUser().toList(mutableListOf())
        }
    job.cancelAndJoin()

    verify(exactly = 1) { firebase.auth.removeAuthStateListener(listener.captured) }
  }

  @Test
  fun signInWithGoogle_returnsSignedInUser_whenAccountIsPicked() = runTest {
    val credential = stubFirebaseCredential()
    pickAccount()
    every { firebase.auth.signInWithCredential(credential) } returns
        completedTask(authResult(firebaseUser(googleUser)))

    val result = repository.signInWithGoogle(context("web-client-id"))

    assertEquals(Result.success(googleUser), result)
    verify(exactly = 1) { firebase.auth.signInWithCredential(credential) }
  }

  @Test
  fun signInWithGoogle_fails_whenWebClientIdIsMissing() = runTest {
    pickAccount()

    val result = repository.signInWithGoogle(context(webClientId = null))

    assertEquals(
        "Google sign-in isn't set up yet - enable the Google provider in the Firebase console, " +
            "then rebuild",
        result.exceptionOrNull()?.message,
    )
    assertTrue(result.exceptionOrNull() is IllegalStateException)
    verify(exactly = 0) { CredentialManager.create(any()) }
  }

  @Test
  fun signInWithGoogle_failsWithHelpfulMessage_whenNoGoogleAccountOnDevice() = runTest {
    pickAccount(error = NoCredentialException("none"))

    val result = repository.signInWithGoogle(context("web-client-id"))

    assertEquals(
        "No Google account found on this device - add one in Settings first",
        result.exceptionOrNull()?.message,
    )
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun signInWithGoogle_failsWithPickerError_whenUserCancels() = runTest {
    val cancellation = GetCredentialCancellationException("cancelled")
    pickAccount(error = cancellation)

    val result = repository.signInWithGoogle(context("web-client-id"))

    assertSame(cancellation, result.exceptionOrNull())
  }

  @Test
  fun signInWithGoogle_fails_whenFirebaseReturnsNoUser() = runTest {
    val credential = stubFirebaseCredential()
    pickAccount()
    every { firebase.auth.signInWithCredential(credential) } returns
        completedTask(authResult(user = null))

    val result = repository.signInWithGoogle(context("web-client-id"))

    assertEquals("Google sign-in succeeded but returned no user", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_fails_whenFirebaseRejectsCredential() = runTest {
    val credential = stubFirebaseCredential()
    pickAccount()
    val rejection = IllegalArgumentException("bad token")
    every { firebase.auth.signInWithCredential(credential) } returns failedTask(rejection)

    val result = repository.signInWithGoogle(context("web-client-id"))

    assertEquals("bad token", result.exceptionOrNull()?.message)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun signOut_signsOutOfFirebase() {
    every { firebase.auth.signOut() } returns Unit

    repository.signOut()

    verify(exactly = 1) { firebase.auth.signOut() }
  }
}
