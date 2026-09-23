package com.github.se.pooltrack.utils

import android.content.Context
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AuthRepository] for unit tests. [googleSignInResult] is what the next
 * [signInWithGoogle] returns; on success, that user becomes the current one.
 */
class FakeAuthRepository(
    initialUser: AuthUser? = null,
    var googleSignInResult: Result<AuthUser> = Result.success(GOOGLE_USER),
) : AuthRepository {

  private val currentUser = MutableStateFlow(initialUser)

  var signInAnonymouslyCalls = 0
    private set

  var signOutCalls = 0
    private set

  override fun getCurrentUser(): Flow<AuthUser?> = currentUser

  override suspend fun signInWithGoogle(context: Context): Result<AuthUser> {
    googleSignInResult.onSuccess { currentUser.value = it }
    return googleSignInResult
  }

  override suspend fun signInAnonymously(): Result<AuthUser> {
    signInAnonymouslyCalls++
    val user = currentUser.value ?: ANONYMOUS_USER
    currentUser.value = user
    return Result.success(user)
  }

  override fun signOut() {
    signOutCalls++
    currentUser.value = null
  }

  companion object {
    val GOOGLE_USER =
        AuthUser(uid = "google-uid", displayName = "Swimmer", email = "a@b.c", isAnonymous = false)
    val ANONYMOUS_USER =
        AuthUser(uid = "anonymous-uid", displayName = null, email = null, isAnonymous = true)
  }
}
