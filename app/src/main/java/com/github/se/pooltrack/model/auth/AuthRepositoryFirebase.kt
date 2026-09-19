package com.github.se.pooltrack.model.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.github.se.pooltrack.model.backup.awaitResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Signs the user in with their Google account, backed by Firebase Authentication. */
class AuthRepositoryFirebase : AuthRepository {

  private val auth: FirebaseAuth = Firebase.auth

  override fun getCurrentUser(): Flow<AuthUser?> = callbackFlow {
    val listener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
          trySend(firebaseAuth.currentUser?.toAuthUser())
        }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  override suspend fun signInWithGoogle(context: Context): Result<AuthUser> = runCatching {
    val webClientId =
        context.defaultWebClientIdOrNull()
            ?: error(
                "Google sign-in isn't set up yet - enable the Google provider in the Firebase " +
                    "console, then rebuild")
    val googleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    val credential = CredentialManager.create(context).getCredential(context, request).credential
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    val firebaseCredential =
        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

    val user =
        auth.signInWithCredential(firebaseCredential).awaitResult().user
            ?: error("Google sign-in succeeded but returned no user")
    user.toAuthUser()
  }

  override suspend fun signInAnonymously(): Result<AuthUser> = runCatching {
    val user =
        auth.currentUser
            ?: auth.signInAnonymously().awaitResult().user
            ?: error("Anonymous sign-in succeeded but returned no user")
    user.toAuthUser()
  }

  override fun signOut() {
    auth.signOut()
  }
}

private fun FirebaseUser.toAuthUser() =
    AuthUser(uid = uid, displayName = displayName, email = email, isAnonymous = isAnonymous)

/**
 * Looks up `R.string.default_web_client_id` by name rather than referencing it directly. The
 * google-services Gradle plugin only generates that resource once `google-services.json` has an
 * OAuth client in it (i.e. once Google sign-in is enabled in the Firebase console) - referencing
 * it directly would fail to compile until then.
 */
private fun Context.defaultWebClientIdOrNull(): String? {
  val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
  return if (resId != 0) getString(resId) else null
}
