package com.github.se.pooltrack.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Entry details screen. Shows the entry picked with [loadEntry], kept up to date
 * as entries and subscriptions change.
 *
 * @property entryRepository The repository used to read and delete entries.
 * @property subscriptionRepository The repository used to read the subscriptions.
 */
class EntryDetailsViewModel(
    private val entryRepository: EntryRepository = EntryRepositoryProvider.repository,
    private val subscriptionRepository: SubscriptionRepository =
        SubscriptionRepositoryProvider.repository,
) : ViewModel() {

  private val selectedTimestamp = MutableStateFlow<Long?>(null)

  /** The selected entry's details, or `null` if none is selected or it no longer exists. */
  val details: StateFlow<EntryDetails?> =
      combine(
              selectedTimestamp,
              entryRepository.getEntries(),
              subscriptionRepository.getSubscriptions(),
          ) { timestamp, entries, subscriptions ->
            timestamp?.let { computeEntryDetails(it, entries, subscriptions) }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** Selects the entry confirmed at [timestampEpochMilli]. */
  fun loadEntry(timestampEpochMilli: Long) {
    selectedTimestamp.value = timestampEpochMilli
  }

  /** Deletes the selected entry, e.g. to undo a misclick on the scanner confirmation. */
  fun onDeleteEntry() {
    val entry = details.value?.entry ?: return
    viewModelScope.launch { entryRepository.deleteEntry(entry) }
  }
}
