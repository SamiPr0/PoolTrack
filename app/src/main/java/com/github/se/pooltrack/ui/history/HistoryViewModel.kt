package com.github.se.pooltrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.entry.timestamp
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.SubscriptionRepository
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.model.subscription.addedAt
import com.github.se.pooltrack.model.subscription.expiresAt
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Why a past entry could not be added. */
enum class AddPastEntryError(val message: String) {
  InTheFuture("That time hasn't happened yet."),
  AlreadyRecorded("There's already an entry at that exact time."),
}

/** The outcome of the last [HistoryViewModel.onAddPastEntry]. */
sealed interface AddPastEntryResult {
  /** The entry was recorded; keeping it lets the UI offer an undo. */
  data class Added(val entry: Entry) : AddPastEntryResult

  data class Refused(val error: AddPastEntryError) : AddPastEntryResult
}

/**
 * ViewModel for the History screen. Exposes every confirmed entry, most recent first, and lets the
 * user back-fill entries they forgot to log.
 *
 * @property repository The repository used to read, add and delete entries.
 * @property subscriptionRepository The repository used to find which subscription a past entry
 *   belongs to.
 */
class HistoryViewModel(
    private val repository: EntryRepository = EntryRepositoryProvider.repository,
    private val subscriptionRepository: SubscriptionRepository =
        SubscriptionRepositoryProvider.repository,
) : ViewModel() {

  val entries: StateFlow<List<Entry>> =
      repository.getEntries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _addPastEntryResult = MutableStateFlow<AddPastEntryResult?>(null)

  /** The outcome of the last [onAddPastEntry], or `null` before one ran or once it was handled. */
  val addPastEntryResult: StateFlow<AddPastEntryResult?> = _addPastEntryResult.asStateFlow()

  /** Removes [entry], e.g. to undo a misclick on the scanner confirmation. */
  fun onDeleteEntry(entry: Entry) {
    viewModelScope.launch { repository.deleteEntry(entry) }
  }

  /**
   * Records an entry that happened at [timestamp], e.g. one that was forgotten at the time. It is
   * refused if [timestamp] is after [now] or an entry already exists at that exact instant (see
   * [addPastEntryResult]).
   */
  fun onAddPastEntry(timestamp: Instant, now: Instant = Instant.now()) {
    viewModelScope.launch {
      _addPastEntryResult.value =
          when {
            timestamp.isAfter(now) -> AddPastEntryResult.Refused(AddPastEntryError.InTheFuture)
            repository.getEntries().first().any { it.timestamp == timestamp } ->
                AddPastEntryResult.Refused(AddPastEntryError.AlreadyRecorded)
            else -> {
              val subscriptions = subscriptionRepository.getSubscriptions().first()
              val entry =
                  Entry(
                      timestampEpochMilli = timestamp.toEpochMilli(),
                      subscriptionId = subscriptionIdAt(timestamp, subscriptions),
                  )
              repository.addEntry(entry)
              AddPastEntryResult.Added(entry)
            }
          }
    }
  }

  /** Clears [addPastEntryResult], once the UI has reacted to it. */
  fun onAddPastEntryResultHandled() {
    _addPastEntryResult.value = null
  }
}

/**
 * The id of the subscription that was valid at [at] (added by then, and not yet expired), or `null`
 * if none was. If several were, the most recently added one wins.
 */
internal fun subscriptionIdAt(at: Instant, subscriptions: List<Subscription>): String? =
    subscriptions
        .filter { !it.addedAt.isAfter(at) && it.expiresAt?.isBefore(at) != true }
        .maxByOrNull { it.addedAtEpochMilli }
        ?.id
