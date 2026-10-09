package com.github.se.pooltrack.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.model.swim.MAX_SWIM_DISTANCE_METERS
import com.github.se.pooltrack.model.swim.parseSwimDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Entry details screen. Shows the entry picked with [loadEntry], kept up to date
 * as entries and subscriptions change.
 *
 * @property entryRepository The repository used to read, change and delete entries.
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

  private val _distanceInput = MutableStateFlow<String?>(null)

  /**
   * The distance being typed while it is edited, as digits, or `null` when no edit is going on. It
   * starts out as the distance logged so far (empty if there is none).
   */
  val distanceInput: StateFlow<String?> = _distanceInput.asStateFlow()

  /** Whether [distanceInput] is a distance that can be saved, see [parseSwimDistance]. */
  val isDistanceInputValid: StateFlow<Boolean> =
      _distanceInput
          .map { it?.let(::parseSwimDistance) != null }
          .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  /** Starts editing the distance of the selected entry. */
  fun onEditDistance() {
    val entry = details.value?.entry ?: return
    _distanceInput.value = entry.swimDistanceMeters?.toString().orEmpty()
  }

  /** Updates the typed distance, dropping anything but digits. */
  fun onDistanceInputChanged(text: String) {
    if (_distanceInput.value == null) return
    _distanceInput.value = sanitizeDistanceInput(text)
  }

  /** Abandons the edit without changing the entry. */
  fun onDistanceEditCancelled() {
    _distanceInput.value = null
  }

  /** Saves the typed distance on the selected entry. Ignored while it is invalid. */
  fun onDistanceEditSaved() {
    val entry = details.value?.entry ?: return
    val meters = _distanceInput.value?.let(::parseSwimDistance) ?: return
    _distanceInput.value = null
    viewModelScope.launch { entryRepository.recordSwimDistance(entry, meters) }
  }
}

/** [text] without anything but digits, cut to the longest distance that can be saved. */
internal fun sanitizeDistanceInput(text: String): String =
    text.filter { it in '0'..'9' }.take(MAX_SWIM_DISTANCE_METERS.toString().length)
