package com.github.se.pooltrack.ui.history.heatmap

import java.time.LocalDate
import java.time.YearMonth

/**
 * Visits in a month that already count as the darkest tile of the year view. Going about three
 * times a week is a full month, so a month with one visit a day is not drowned out by the scale.
 */
const val BUSY_MONTH_VISITS = 12

/**
 * One tile of the year view.
 *
 * @property count Entries in the month.
 * @property level The intensity step from 0 to [MAX_LEVEL].
 * @property isFuture Whether the month hasn't started yet.
 */
data class MonthTile(val month: YearMonth, val count: Int, val level: Int, val isFuture: Boolean)

/** Sums [countsByDay] per month. */
fun monthCounts(countsByDay: Map<LocalDate, Int>): Map<YearMonth, Int> =
    countsByDay.entries.groupingBy { YearMonth.from(it.key) }.fold(0) { sum, e -> sum + e.value }

/**
 * The twelve tiles of [year], January first.
 *
 * @param monthCounts Entries per month, see [monthCounts].
 * @param maxCount The busiest month to scale against; pass the same value for every year so that
 *   colors mean the same thing from one year to the next.
 */
fun buildYearTiles(
    year: Int,
    monthCounts: Map<YearMonth, Int>,
    today: LocalDate,
    maxCount: Int = monthCounts.values.maxOrNull() ?: 0,
): List<MonthTile> {
  val thisMonth = YearMonth.from(today)
  return (1..12).map { monthOfYear ->
    val month = YearMonth.of(year, monthOfYear)
    val count = monthCounts[month] ?: 0
    MonthTile(
        month = month,
        count = count,
        level = intensityLevel(count, maxCount, scaleFloor = BUSY_MONTH_VISITS),
        isFuture = month.isAfter(thisMonth),
    )
  }
}

/** Every year from [first] to [last], oldest first; just [last] if [first] is after it. */
fun yearsBetween(first: Int, last: Int): List<Int> =
    if (first > last) listOf(last) else (first..last).toList()

/** "5 visits" (or "1 visit"). */
fun visitsLabel(count: Int): String = if (count == 1) "1 visit" else "$count visits"

/**
 * The month to open when zooming in from [year]: [current] if it is in that year, otherwise the
 * latest of [months] in it (the year may have been swiped to meanwhile), or [current] if none is.
 */
fun monthToFocus(current: YearMonth, year: Int, months: List<YearMonth>): YearMonth =
    if (current.year == year) current else months.lastOrNull { it.year == year } ?: current
