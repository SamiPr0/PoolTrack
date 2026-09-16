package com.github.se.pooltrack.ui.subscription

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocal
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel shared by the Subscription and "manage subscriptions" screens. Manages the list of
 * subscription PDFs the user has added, which one is active, and records a confirmed entry once
 * the scanner accepts a scan.
 */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

  private val subscriptionRepository: SubscriptionRepository =
      SubscriptionRepositoryLocal(application)
  private val entryRepository: EntryRepository = EntryRepositoryLocal(application)

  val subscriptions: StateFlow<List<Subscription>> =
      subscriptionRepository
          .getSubscriptions()
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val activeSubscription: StateFlow<Subscription?> =
      subscriptionRepository
          .getActiveSubscription()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /**
   * Adds the PDF the user just picked as a new subscription and makes it the active one, taking a
   * long-lived read permission on it so it can still be opened after the app or device restarts.
   *
   * @param uri The content URI returned by the document picker.
   */
  fun onSubscriptionPicked(uri: Uri) {
    getApplication<Application>()
        .contentResolver
        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val displayName = queryDisplayName(uri)
    viewModelScope.launch { subscriptionRepository.addSubscription(uri.toString(), displayName) }
  }

  /**
   * Makes the subscription with [id] the active one.
   *
   * @param id The identifier of the subscription to activate.
   */
  fun onSetActive(id: String) {
    viewModelScope.launch { subscriptionRepository.setActiveSubscription(id) }
  }

  /**
   * Removes a subscription, e.g. an expired one that's no longer needed.
   *
   * @param id The identifier of the subscription to remove.
   */
  fun onDeleteSubscription(id: String) {
    viewModelScope.launch { subscriptionRepository.deleteSubscription(id) }
  }

  /**
   * Records a confirmed entry. Call this only when the scanner accepted the scan, i.e. the user
   * actually entered the pool. The reopen cooldown is derived from this same entry list (see
   * [com.github.se.pooltrack.ui.home.HomeStats]), so there is nothing extra to record here.
   */
  fun onScannerAccepted() {
    viewModelScope.launch { entryRepository.addEntry(Entry(Instant.now())) }
  }

  /** Looks up [uri]'s display name via the content provider, falling back to a generic label. */
  private fun queryDisplayName(uri: Uri): String {
    val resolver = getApplication<Application>().contentResolver
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex >= 0 && cursor.moveToFirst()) {
        cursor.getString(nameIndex)?.let {
          return it
        }
      }
    }
    return "Subscription"
  }
}
