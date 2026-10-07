package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.isSameEntryAs
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * Reminds the user, once, to log how far they swam: the first time they pick their phone back up
 * after the cancel window of their entry. Knows nothing about notifications or the Android
 * lifecycle: whoever detects the unlock calls [onPhoneUnlocked], and [remind] shows the reminder.
 *
 * @property entryRepository Where the entries live.
 * @property remind Shows the reminder to the user.
 */
class SwimReporter(
    private val entryRepository: EntryRepository,
    private val remind: () -> Unit,
) {

  private var started = false
  private var tracked: Entry? = null
  private var reminded = false

  /**
   * Starts tracking the most recent entry, if it is still waiting for a distance. From now on only
   * that entry is ever reminded about: if it is removed, nothing is, rather than falling back to an
   * older one.
   *
   * @return `true` if there is a distance to wait for.
   */
  suspend fun startTracking(): Boolean {
    val latest = entryRepository.getEntries().first().firstOrNull()
    started = true
    reminded = false
    tracked = latest?.takeIf { isSwimPending(it) }
    return tracked != null
  }

  /**
   * Handles the user unlocking their phone at [now].
   *
   * @return `true` if the reminder was shown, `false` if there was nothing to remind about: the
   *   tracked entry was removed or logged, it is still in its cancel window, or the reminder was
   *   already shown.
   */
  suspend fun onPhoneUnlocked(now: Instant = Instant.now()): Boolean {
    val entry = currentTrackedEntry() ?: return false
    if (reminded || !isLogReminderDue(entry, now)) return false
    reminded = true
    remind()
    return true
  }

  /** Whether the tracked entry still waits for its distance and has not been reminded about. */
  suspend fun hasPendingSwim(): Boolean = !reminded && isSwimPending(currentTrackedEntry())

  /**
   * Suspends until the tracked entry is gone (cancelled) or got its distance (returns at once if
   * nothing is tracked).
   */
  suspend fun awaitTrackedEntryResolved() {
    val entry = tracked ?: return
    entryRepository.getEntries().first { entries ->
      entries.none { it.isSameEntryAs(entry) && it.awaitingDistance }
    }
  }

  /** The stored version of the tracked entry, or `null` if it was removed. */
  private suspend fun currentTrackedEntry(): Entry? {
    if (!started) startTracking()
    val entry = tracked ?: return null
    return entryRepository.getEntries().first().firstOrNull { it.isSameEntryAs(entry) }
  }
}
