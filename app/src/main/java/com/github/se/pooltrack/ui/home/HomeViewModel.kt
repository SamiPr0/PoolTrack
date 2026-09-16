package com.github.se.pooltrack.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocal
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Home screen. Exposes stats derived from the confirmed entries, and when the
 * subscription pass was last shown (so Home can gate reopening it during its cooldown).
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

  val lastSubscriptionOpenAt: StateFlow<Instant?> =
      subscriptionRepository
          .getLastOpenedAt()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
