package com.github.se.pooltrack.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel shared by the Subscription and "manage subscriptions" screens. Manages the list of
 * subscription PDFs the user has added, which one is active, and records a confirmed entry once the
 * scanner accepts a scan.
 *
 * @property subscriptionRepository The repository used to read and manage the subscriptions.
 * @property entryRepository The repository used to read and record the confirmed entries.
 */
class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository =
        SubscriptionRepositoryProvider.repository,
    private val entryRepository: EntryRepository = EntryRepositoryProvider.repository,
) : ViewModel() {

  val subscriptions: StateFlow<List<Subscription>> =
      subscriptionRepository
          .getSubscriptions()
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val activeSubscription: StateFlow<Subscription?> =
      subscriptionRepository
          .getActiveSubscription()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /**
   * The timestamp of the most recent confirmed entry, or `null` if there is none. This is what the
   * reopen cooldown is measured from - see
   * [com.github.se.pooltrack.model.subscription.remainingSubscriptionCooldown].
   */
  val lastEntryTimestamp: StateFlow<Instant?> =
      entryRepository.getLastEntryTimestamp().stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** How many confirmed entries each subscription has, keyed by subscription id. */
  val entryCountsBySubscriptionId: StateFlow<Map<String, Int>> =
      entryRepository
          .getEntries()
          .map { entries -> entries.mapNotNull { it.subscriptionId }.groupingBy { it }.eachCount() }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  /**
   * Adds the PDF the user just picked as a new subscription and makes it the active one.
   *
   * @param uri The content URI returned by the document picker, as a string.
   * @param expiresAtEpochMilli When it expires, as epoch milliseconds, or `null` for no date-based
   *   expiration.
   * @param maxEntries The number of entries it's good for, or `null` for no entry-count limit.
   * @param price How much it cost, or `null` if not recorded.
   */
  fun onSubscriptionPicked(
      uri: String,
      expiresAtEpochMilli: Long? = null,
      maxEntries: Int? = null,
      price: Double? = null,
  ) {
    viewModelScope.launch {
      subscriptionRepository.addSubscription(uri, expiresAtEpochMilli, maxEntries, price)
    }
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
   * [com.github.se.pooltrack.ui.home.HomeStats]), so there is nothing extra to record here. The
   * entry is tagged with whichever subscription is active right now, so an entry-limited
   * subscription can count how many of its entries have actually been used.
   */
  fun onScannerAccepted() {
    val subscriptionId = activeSubscription.value?.id
    val entry =
        Entry(timestampEpochMilli = Instant.now().toEpochMilli(), subscriptionId = subscriptionId)
    viewModelScope.launch { entryRepository.addEntry(entry) }
  }
}
