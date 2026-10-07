package com.github.se.pooltrack.ui.history.heatmap

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * One day of a [MonthGrid].
 *
 * @property level The intensity step from 0 to [MAX_LEVEL], see [intensityLevel].
 * @property isFuture Whether the day is after today, so nothing can have happened yet.
 */
data class MonthDay(
    val date: LocalDate,
    val count: Int,
    val level: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/**
 * A month laid out like a wall calendar: [weeks] of seven slots each, starting on a given weekday,
 * with `null` for the slots before the 1st and after the last day.
 */
data class MonthGrid(val month: YearMonth, val weeks: List<List<MonthDay?>>) {
  /** Entries confirmed during the month. */
  val visits: Int
    get() = weeks.sumOf { week -> week.sumOf { it?.count ?: 0 } }
}

/**
 * Lays out [month].
 *
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param maxCount The busiest day to scale the intensity against; pass the same value for every
 *   month so that colors mean the same thing from one month to the next.
 */
fun buildMonthGrid(
    month: YearMonth,
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    maxCount: Int = countsByDay.values.maxOrNull() ?: 0,
): MonthGrid {
  val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
  val slots: List<MonthDay?> =
      List(leadingBlanks) { null } +
          (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val count = countsByDay[date] ?: 0
            MonthDay(
                date = date,
                count = count,
                level = intensityLevel(count, maxCount),
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
          }
  val padded = slots + List((7 - slots.size % 7) % 7) { null }
  return MonthGrid(month, padded.chunked(7))
}

/** Every month from [first] to [last], oldest first; just [last] if [first] is after it. */
fun monthsBetween(first: YearMonth, last: YearMonth): List<YearMonth> {
  if (first.isAfter(last)) return listOf(last)
  return generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(last) }.toList()
}
