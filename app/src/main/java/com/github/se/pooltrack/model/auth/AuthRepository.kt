package com.github.se.pooltrack.model.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Represents a repository that manages the signed-in user, used to scope the Firestore backup. */
interface AuthRepository {

  /** Emits the currently signed-in user, or `null` if signed out. */
  fun getCurrentUser(): Flow<AuthUser?>

  /**
   * Signs the user in with their Google account via Android's Credential Manager.
   *
   * @param context Used to launch the system account picker; must be an Activity context.
   */
  suspend fun signInWithGoogle(context: Context): Result<AuthUser>

  /**
   * Signs the user in anonymously - only meant for local development builds, as a way to reach
   * the app without setting up a real Google account on an emulator/device. A no-op returning the
   * existing user if someone is already signed in, anonymously or otherwise.
   */
  suspend fun signInAnonymously(): Result<AuthUser>

  /** Signs the current user out. */
  fun signOut()
}
