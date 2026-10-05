package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.EntryRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * Reports how long the swim lasted, once, the first time the user picks their phone back up after
 * it. Knows nothing about notifications or the Android lifecycle: whoever detects the unlock calls
 * [onPhoneUnlocked], and [report] shows the result.
 *
 * @property entryRepository Where the entries (and the recorded swim durations) live.
 * @property report Shows the swim duration to the user.
 */
class SwimReporter(
    private val entryRepository: EntryRepository,
    private val report: (Duration) -> Unit,
) {

  /**
   * Handles the user unlocking their phone at [now].
   *
   * @return `true` if a swim duration was reported and recorded, `false` if there was nothing to
   *   report (see [swimDurationToReport]).
   */
  suspend fun onPhoneUnlocked(now: Instant = Instant.now()): Boolean {
    val lastEntry = entryRepository.getEntries().first().firstOrNull()
    val duration = swimDurationToReport(lastEntry, now) ?: return false
    entryRepository.recordSwimDuration(checkNotNull(lastEntry), duration.toMillis())
    report(duration)
    return true
  }

  /** Whether the most recent entry could still be reported, see [isSwimPending]. */
  suspend fun hasPendingSwim(now: Instant = Instant.now()): Boolean =
      isSwimPending(entryRepository.getEntries().first().firstOrNull(), now)
}
