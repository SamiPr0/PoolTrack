package com.github.se.pooltrack.ui.account

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryFirebase
import com.github.se.pooltrack.model.auth.AuthUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the sign-in/sign-out control on Home. Signing in is what lets subscriptions and
 * pool entries be backed up to Firestore - the local repositories mirror to it whenever a user is
 * signed in, and silently skip the mirror otherwise.
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

  private val authRepository: AuthRepository = AuthRepositoryFirebase()

  val currentUser: StateFlow<AuthUser?> =
      authRepository.getCurrentUser().stateIn(viewModelScope, SharingStarted.Eagerly, null)

  init {
    // Bootstraps a session immediately so backup starts working without requiring the user to
    // tap anything first - signInAnonymously() is a no-op if someone's already signed in. Ignored
    // on failure (e.g. anonymous auth not yet enabled in the Firebase console): the app works
    // exactly as if nobody were signed in, same as before this existed.
    viewModelScope.launch { authRepository.signInAnonymously() }
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
