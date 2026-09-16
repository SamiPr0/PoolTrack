package com.github.se.pooltrack.ui.subscription

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the Subscription screen. Manages the picked subscription PDF's URI. */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: SubscriptionRepository = SubscriptionRepositoryLocal(application)

  val subscriptionUri: StateFlow<String?> =
      repository.getSubscriptionUri().stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /**
   * Persists the PDF the user just picked, taking a long-lived read permission on it so it can
   * still be opened after the app or device restarts.
   *
   * @param uri The content URI returned by the document picker.
   */
  fun onSubscriptionPicked(uri: Uri) {
    getApplication<Application>()
        .contentResolver
        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    viewModelScope.launch { repository.setSubscriptionUri(uri.toString()) }
  }
}
