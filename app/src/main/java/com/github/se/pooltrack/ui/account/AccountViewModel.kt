package com.github.se.pooltrack.ui.account

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.BuildConfig
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryFirebase
import com.github.se.pooltrack.model.auth.AuthUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel backing both [SignInScreen] and Home's account control. Signing in is required to use
 * the app at all - see `PoolTrackApp` in `MainActivity.kt` - since subscriptions and entries are
 * backed up to the signed-in account, and the local repositories mirror to it whenever a user is
 * signed in.
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

  private val authRepository: AuthRepository = AuthRepositoryFirebase()

  val currentUser: StateFlow<AuthUser?> =
      authRepository.getCurrentUser().stateIn(viewModelScope, SharingStarted.Eagerly, null)

  init {
    // Debug builds only: bootstraps an anonymous session automatically, so local development
    // doesn't require setting up a real Google account on an emulator/device. Release builds
    // still hard-require real Google sign-in - see the gate in MainActivity.kt.
    if (BuildConfig.DEBUG) {
      viewModelScope.launch { authRepository.signInAnonymously() }
    }
  }

  /**
   * Starts the Google sign-in flow. [context] must be an Activity context, since it's used to
   * launch the system account picker. Calls [onError] with a short message if it fails or is
   * cancelled, so the caller can surface it (e.g. as a Snackbar).
   */
  fun onSignInClick(context: Context, onError: (String) -> Unit) {
    viewModelScope.launch {
      authRepository.signInWithGoogle(context).onFailure { error ->
        onError(error.message ?: "Sign-in failed")
      }
    }
  }

  /** Signs the current user out. Local data stays untouched - only the cloud backup stops. */
  fun onSignOutClick() {
    authRepository.signOut()
  }
}
