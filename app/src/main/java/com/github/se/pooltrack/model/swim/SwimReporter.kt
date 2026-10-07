package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.isSameEntryAs
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

  private var started = false
  private var tracked: Entry? = null

  /**
   * Starts tracking the most recent entry, if its swim could still be reported. From now on only
   * that entry is ever reported: if it is removed, nothing is, rather than falling back to an older
   * one.
   *
   * @return `true` if there is a swim to wait for.
   */
  suspend fun startTracking(now: Instant = Instant.now()): Boolean {
    val latest = entryRepository.getEntries().first().firstOrNull()
    started = true
    tracked = latest?.takeIf { isSwimPending(it, now) }
    return tracked != null
  }

  /**
   * Handles the user unlocking their phone at [now].
   *
   * @return `true` if a swim duration was reported and recorded, `false` if there was nothing to
   *   report (see [swimDurationToReport]), including when the tracked entry was removed.
   */
  suspend fun onPhoneUnlocked(now: Instant = Instant.now()): Boolean {
    val entry = currentTrackedEntry(now) ?: return false
    val duration = swimDurationToReport(entry, now) ?: return false
    entryRepository.recordSwimDuration(entry, duration.toMillis())
    // The entry may have been removed meanwhile, in which case nothing was recorded.
    if (currentTrackedEntry(now)?.swimDurationMillis == null) return false
    report(duration)
    return true
  }

  /** Whether the tracked entry still exists and could still be reported, see [isSwimPending]. */
  suspend fun hasPendingSwim(now: Instant = Instant.now()): Boolean =
      isSwimPending(currentTrackedEntry(now), now)

  /** Suspends until the tracked entry no longer exists (returns at once if nothing is tracked). */
  suspend fun awaitTrackedEntryRemoved() {
    val entry = tracked ?: return
    entryRepository.getEntries().first { entries -> entries.none { it.isSameEntryAs(entry) } }
  }

  /** The stored version of the tracked entry, or `null` if it was removed. */
  private suspend fun currentTrackedEntry(now: Instant): Entry? {
    if (!started) startTracking(now)
    val entry = tracked ?: return null
    return entryRepository.getEntries().first().firstOrNull { it.isSameEntryAs(entry) }
  }
}
