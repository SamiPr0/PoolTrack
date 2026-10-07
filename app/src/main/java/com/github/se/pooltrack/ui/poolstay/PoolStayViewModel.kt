package com.github.se.pooltrack.ui.poolstay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.entry.timestamp
import com.github.se.pooltrack.model.swim.parseSwimDistance
import com.github.se.pooltrack.model.swim.remainingCancelWindow
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the app shows right now, as far as the user's current stay in the pool is concerned. */
sealed interface PoolStayState {

  /** The entries were not read yet, so it is not known whether a stay is going on. */
  data object Loading : PoolStayState

  /** No stay going on: the normal app is shown. */
  data object None : PoolStayState

  /** The entry was just confirmed and can still be cancelled for [remaining]. */
  data class CancelWindow(val entry: Entry, val elapsed: Duration, val remaining: Duration) :
      PoolStayState

  /** The cancel window is over: the user has to log how far they swam to get back to the app. */
  data class LogRequired(val entry: Entry) : PoolStayState
}

/**
 * The [PoolStayState] at [now]. Only the latest entry matters, and only while it still awaits its
 * distance, so entries added by hand, imported or recorded before distances existed never get the
 * user stuck. Derived from the entry's own timestamp, so it survives the app being killed.
 */
fun computePoolStayState(entries: List<Entry>, now: Instant): PoolStayState {
  val latest = entries.maxByOrNull { it.timestampEpochMilli }
  if (latest == null || !latest.awaitingDistance) return PoolStayState.None
  val remaining = remainingCancelWindow(latest, now) ?: return PoolStayState.LogRequired(latest)
  return PoolStayState.CancelWindow(latest, Duration.between(latest.timestamp, now), remaining)
}

private const val MAX_INPUT_LENGTH = 6
private val ONE_SECOND = Duration.ofSeconds(1)

private fun everySecond(): Flow<Unit> = flow {
  while (true) {
    emit(Unit)
    delay(ONE_SECOND.toMillis())
  }
}

/**
 * ViewModel of the "in the pool" screen: right after a scan is confirmed the user can cancel the
 * entry for 10 minutes, then has to log how far they swam. Also decides whether this screen is
 * shown at all, see [state].
 *
 * @property entryRepository The repository used to read, cancel and complete the entry.
 * @property clock Where "now" comes from; replaced in tests.
 * @param ticker Emits whenever [state] should be recomputed, i.e. as time passes; replaced in
 *   tests.
 */
class PoolStayViewModel(
    private val entryRepository: EntryRepository = EntryRepositoryProvider.repository,
    private val clock: () -> Instant = Instant::now,
    ticker: Flow<Unit> = everySecond(),
) : ViewModel() {

  /** What to show now; moves from [PoolStayState.CancelWindow] to [PoolStayState.LogRequired]. */
  val state: StateFlow<PoolStayState> =
      combine(entryRepository.getEntries(), ticker) { entries, _ ->
            computePoolStayState(entries, clock())
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, PoolStayState.Loading)

  private val _distanceInput = MutableStateFlow("")

  /** The distance the user is typing, digits only. */
  val distanceInput: StateFlow<String> = _distanceInput

  /** Whether [distanceInput] is a distance that can be saved, see [parseSwimDistance]. */
  val isDistanceValid: StateFlow<Boolean> =
      _distanceInput
          .map { parseSwimDistance(it) != null }
          .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  /** Updates the typed distance, dropping anything but digits. */
  fun onDistanceChanged(text: String) {
    _distanceInput.value = text.filter { it in '0'..'9' }.take(MAX_INPUT_LENGTH)
  }

  /**
   * Cancels the entry, e.g. when the scanner declined the scan after all, so it does not count.
   * Ignored once the cancel window is over.
   */
  fun onCancelEntry() {
    val current = state.value as? PoolStayState.CancelWindow ?: return
    if (remainingCancelWindow(current.entry, clock()) == null) return
    viewModelScope.launch { entryRepository.deleteEntry(current.entry) }
  }

  /** Stores the typed distance and ends the stay. Ignored while it is invalid or not required. */
  fun onSaveDistance() {
    val current = state.value as? PoolStayState.LogRequired ?: return
    val meters = parseSwimDistance(_distanceInput.value) ?: return
    viewModelScope.launch {
      entryRepository.recordSwimDistance(current.entry, meters)
      _distanceInput.value = ""
    }
  }
}
