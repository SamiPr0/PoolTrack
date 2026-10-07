package com.github.se.pooltrack.model.swim

import java.time.Duration

/**
 * Formats [duration] for display, e.g. `1h 12min` or `45min`. Only old entries have one: swims are
 * no longer timed, the user logs a distance instead (see [formatSwimDistance]).
 */
fun formatSwimDuration(duration: Duration): String {
  val hours = duration.toHours()
  val minutes = duration.toMinutes() % 60
  return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}
