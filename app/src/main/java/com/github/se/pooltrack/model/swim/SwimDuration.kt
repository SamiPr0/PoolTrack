package com.github.se.pooltrack.model.swim

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import java.time.Duration
import java.time.Instant

/**
 * Phone unlocks this soon after an entry are ignored: the user is still at the entrance or in the
 * changing room, not done swimming.
 */
val MIN_SWIM_DURATION: Duration = Duration.ofMinutes(10)

/**
 * The swim duration to report for the user's most recent entry, or `null` if there is nothing to
 * report right now.
 *
 * @param lastEntry The most recent confirmed entry, or `null` if there is none.
 * @param now The instant the user picked their phone back up.
 * @return `now` minus the entry time, unless there is no entry, it was already reported, or it is
 *   still shorter than [MIN_SWIM_DURATION].
 */
fun swimDurationToReport(lastEntry: Entry?, now: Instant = Instant.now()): Duration? {
  if (lastEntry == null || lastEntry.swimDurationMillis != null) return null
  val elapsed = Duration.between(lastEntry.timestamp, now)
  return if (elapsed < MIN_SWIM_DURATION) null else elapsed
}

/** Formats [duration] for display, e.g. `1h 12min` or `45min`. */
fun formatSwimDuration(duration: Duration): String {
  val hours = duration.toHours()
  val minutes = duration.toMinutesPart()
  return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}
