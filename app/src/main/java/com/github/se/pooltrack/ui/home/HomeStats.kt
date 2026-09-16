package com.github.se.pooltrack.ui.home

import com.github.se.pooltrack.model.entry.Entry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Summary statistics derived from the confirmed entries.
 *
 * @property totalEntries How many entries were ever confirmed.
 * @property entriesThisWeek How many entries fall within the current calendar week.
 * @property daysSinceLastSwim Whole days since the most recent entry, or `null` if there is none.
 */
data class HomeStats(
    val totalEntries: Int,
    val entriesThisWeek: Int,
    val daysSinceLastSwim: Long?,
)

/** Computes [HomeStats] for [entries] as of [now], using [zone] to resolve calendar days/weeks. */
fun computeHomeStats(
    entries: List<Entry>,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): HomeStats {
  val today = LocalDate.ofInstant(now, zone)
  val weekFields = WeekFields.of(Locale.getDefault())
  val startOfWeek = today.with(weekFields.dayOfWeek(), 1L)
  val startOfWeekInstant = startOfWeek.atStartOfDay(zone).toInstant()

  val entriesThisWeek = entries.count { !it.timestamp.isBefore(startOfWeekInstant) }
  val daysSinceLastSwim =
      entries.maxOfOrNull { it.timestamp }?.let { lastEntry ->
        ChronoUnit.DAYS.between(LocalDate.ofInstant(lastEntry, zone), today)
      }

  return HomeStats(
      totalEntries = entries.size,
      entriesThisWeek = entriesThisWeek,
      daysSinceLastSwim = daysSinceLastSwim,
  )
}
