package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import java.time.Duration
import java.time.Instant

/**
 * How long after confirming an entry the user can still cancel it. Afterwards they have to log how
 * far they swam.
 */
val CANCEL_WINDOW: Duration = Duration.ofMinutes(10)

/** The longest distance the user can log, in metres. Nobody swims further in one visit. */
const val MAX_SWIM_DISTANCE_METERS = 50_000

/**
 * The distance the user typed, or `null` if it is not a whole number of metres between 1 and
 * [MAX_SWIM_DISTANCE_METERS]. Surrounding whitespace is ignored.
 */
fun parseSwimDistance(input: String): Int? =
    input
        .trim()
        .takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }
        ?.toIntOrNull()
        ?.takeIf { it in 1..MAX_SWIM_DISTANCE_METERS }

/** Formats [meters] for display, e.g. `1200 m`. */
fun formatSwimDistance(meters: Int): String = "$meters m"

/** Whether [entry] still has to get its distance logged (and was not cancelled). */
fun isSwimPending(entry: Entry?): Boolean = entry != null && entry.awaitingDistance

/** The time left to cancel [entry] at [now], or `null` once [CANCEL_WINDOW] is over. */
fun remainingCancelWindow(entry: Entry, now: Instant): Duration? {
  val remaining = CANCEL_WINDOW - Duration.between(entry.timestamp, now)
  return if (remaining.isNegative || remaining.isZero) null else remaining
}

/**
 * Whether the user should be reminded to log [entry]'s distance at [now]: it is still waiting for
 * one and the cancel window is over. There is deliberately no upper time limit, logging is
 * mandatory.
 */
fun isLogReminderDue(entry: Entry?, now: Instant): Boolean =
    entry != null && entry.awaitingDistance && remainingCancelWindow(entry, now) == null
