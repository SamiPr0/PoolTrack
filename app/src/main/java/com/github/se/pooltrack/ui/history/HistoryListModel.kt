package com.github.se.pooltrack.ui.history

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import com.github.se.pooltrack.model.swim.formatSwimDistance
import com.github.se.pooltrack.model.swim.formatSwimDuration
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** One line of the history list below the overview card. */
sealed interface HistoryRow {
  /** A stable identity for the list. */
  val key: Any

  /**
   * The header of the week starting on [start].
   *
   * @property visits Entries in the week.
   * @property meters The distance logged during the week, in metres.
   */
  data class Week(val start: LocalDate, val visits: Int, val meters: Int) : HistoryRow {
    override val key: Any = "week-$start"
  }

  /** An [entry] confirmed on [day]. */
  data class Item(val day: LocalDate, val entry: Entry) : HistoryRow {
    override val key: Any = entry.timestampEpochMilli
  }
}

/**
 * Lays out [entries] as a week header followed by that week's entries, most recent first. Days are
 * resolved in [zone] and weeks start on [firstDayOfWeek].
 */
fun buildHistoryRows(
    entries: List<Entry>,
    firstDayOfWeek: DayOfWeek,
    zone: ZoneId = ZoneId.systemDefault(),
): List<HistoryRow> =
    entries
        .sortedByDescending { it.timestamp }
        .map { HistoryRow.Item(it.timestamp.atZone(zone).toLocalDate(), it) }
        .groupBy { weekStart(it.day, firstDayOfWeek) }
        .flatMap { (start, items) ->
          listOf(
              HistoryRow.Week(
                  start = start,
                  visits = items.size,
                  meters = items.sumOf { it.entry.swimDistanceMeters ?: 0 },
              )
          ) + items
        }

/** The position of the first (most recent) entry of [day] in these rows, or `null` if none. */
fun List<HistoryRow>.indexOfDay(day: LocalDate): Int? = indexOfFirst {
  it is HistoryRow.Item && it.day == day
}
    .takeIf { it >= 0 }

/**
 * What a row of the list says about an entry. The distance leads when it was logged, since it is
 * what swims are compared by; otherwise the time does.
 *
 * @property primary The bold line: the distance, or the time if no distance was logged.
 * @property secondary The lighter line: the time (when the distance leads) and the swim duration;
 *   `null` when there is nothing more to say.
 */
data class EntryRowTexts(val primary: String, val secondary: String?)

/**
 * The [EntryRowTexts] for [entry], with its time already formatted as [time].
 *
 * Examples: "1200 m" over "6:42 PM · 52min"; just "6:42 PM" over "52min" for an entry without a
 * distance; just "6:42 PM" when it has neither.
 */
fun entryRowTexts(entry: Entry, time: String): EntryRowTexts {
  val duration = entry.swimDurationMillis?.let { formatSwimDuration(Duration.ofMillis(it)) }
  val distance = entry.swimDistanceMeters?.let { formatSwimDistance(it) }
  return if (distance != null) {
    EntryRowTexts(distance, listOfNotNull(time, duration).joinToString(" · "))
  } else {
    EntryRowTexts(time, duration)
  }
}

/** "850 m" under a kilometre, otherwise kilometres with one decimal ("4.2 km"). */
fun formatTotalDistance(meters: Int, locale: Locale = Locale.getDefault()): String =
    if (meters < 1000) "$meters m" else String.format(locale, "%.1f km", meters / 1000.0)
