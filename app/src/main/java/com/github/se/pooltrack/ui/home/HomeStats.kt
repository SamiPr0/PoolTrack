package com.github.se.pooltrack.ui.home

import com.github.se.pooltrack.model.entry.Entry
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Summary statistics derived from the confirmed entries.
 *
 * @property totalEntries How many entries were ever confirmed.
 * @property entriesThisWeek How many entries fall within the current calendar week.
 * @property entriesThisMonth How many entries fall within the current calendar month.
 * @property daysSinceLastSwim Whole days since the most recent entry, or `null` if there is none.
 * @property averageEntriesPerWeek Entries per calendar week since the first one, or `null` if
 *   there is none.
 * @property favoriteDayOfWeek The day of the week with the most entries, or `null` if there is
 *   none.
 * @property lastEntryTimestamp The exact instant of the most recent entry, or `null` if there is
 *   none. This is the single source of truth for "when did I last enter the pool" - derived from
 *   the entries themselves so that deleting the latest one (e.g. to undo a misclick) immediately
 *   reflects in it, instead of drifting out of sync with a separately stored value.
 */
data class HomeStats(
    val totalEntries: Int,
    val entriesThisWeek: Int,
    val entriesThisMonth: Int,
    val daysSinceLastSwim: Long?,
    val averageEntriesPerWeek: Double?,
    val favoriteDayOfWeek: DayOfWeek?,
    val lastEntryTimestamp: Instant?,
)

/** Computes [HomeStats] for [entries] as of [now], using [zone] to resolve calendar days/weeks. */
fun computeHomeStats(
    entries: List<Entry>,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): HomeStats {
  val today = now.atZone(zone).toLocalDate()
  val weekFields = WeekFields.of(Locale.getDefault())
  val startOfWeek = today.with(weekFields.dayOfWeek(), 1L)
  val startOfWeekInstant = startOfWeek.atStartOfDay(zone).toInstant()
  val thisMonth = YearMonth.from(today)

  val entriesThisWeek = entries.count { !it.timestamp.isBefore(startOfWeekInstant) }
  val entriesThisMonth =
      entries.count { YearMonth.from(it.timestamp.atZone(zone).toLocalDate()) == thisMonth }

  val entryDates = entries.map { it.timestamp.atZone(zone).toLocalDate() }
  val lastEntryTimestamp = entries.maxOfOrNull { it.timestamp }
  val daysSinceLastSwim = entryDates.maxOrNull()?.let { ChronoUnit.DAYS.between(it, today) }
  val averageEntriesPerWeek =
      entryDates.minOrNull()?.let { firstEntry ->
        val weeksElapsed = ChronoUnit.WEEKS.between(firstEntry, today) + 1
        entries.size.toDouble() / weeksElapsed
      }
  val favoriteDayOfWeek =
      entryDates.groupingBy { it.dayOfWeek }.eachCount().maxByOrNull { it.value }?.key

  return HomeStats(
      totalEntries = entries.size,
      entriesThisWeek = entriesThisWeek,
      entriesThisMonth = entriesThisMonth,
      daysSinceLastSwim = daysSinceLastSwim,
      averageEntriesPerWeek = averageEntriesPerWeek,
      favoriteDayOfWeek = favoriteDayOfWeek,
      lastEntryTimestamp = lastEntryTimestamp,
  )
}
