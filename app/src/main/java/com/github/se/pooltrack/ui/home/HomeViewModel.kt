package com.github.se.pooltrack.ui.home

import android.app.Application
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Home screen. Exposes stats derived from the confirmed entries, when the user
 * last entered the pool (so Home can gray out its "open subscription" shortcut during the reopen
 * cooldown, same rule the Subscription screen itself enforces), and money/expiration stats derived
 * from the subscriptions themselves.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val entryRepository: EntryRepository = EntryRepositoryLocal(application)
  private val subscriptionRepository: SubscriptionRepository =
      SubscriptionRepositoryLocal(application)

  val stats: StateFlow<HomeStats> =
      entryRepository
          .getEntries()
          .map { entries: List<Entry> -> computeHomeStats(entries) }
          .stateIn(viewModelScope, SharingStarted.Eagerly, computeHomeStats(emptyList()))

  val lastEntryTimestamp: StateFlow<Instant?> =
      entryRepository
          .getLastEntryTimestamp()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val activeSubscription: StateFlow<Subscription?> =
      subscriptionRepository
          .getActiveSubscription()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** The sum of every recorded subscription price, ignoring those with no price recorded. */
  val totalSpent: StateFlow<Double> =
      subscriptionRepository
          .getSubscriptions()
          .map { subscriptions -> subscriptions.sumOf { it.price ?: 0.0 } }
          .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

  /** How many confirmed entries were recorded against the currently active subscription. */
  val activeSubscriptionUsedEntries: StateFlow<Int> =
      combine(entryRepository.getEntries(), activeSubscription) { entries, active ->
            if (active == null) 0 else entries.count { it.subscriptionId == active.id }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
