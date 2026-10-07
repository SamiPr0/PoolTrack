package com.github.se.pooltrack.ui.history.heatmap

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Number of intensity steps above "no visit"; levels run from 0 (none) to [MAX_LEVEL]. */
const val MAX_LEVEL = 4

/** Weeks shown by default, so the grid covers a little over a year like GitHub's. */
const val DEFAULT_WEEK_COUNT = 53

/**
 * One cell of the heatmap.
 *
 * @property date The calendar day.
 * @property count How many entries were confirmed that day.
 * @property level The intensity step from 0 to [MAX_LEVEL], see [intensityLevel].
 */
data class HeatmapDay(val date: LocalDate, val count: Int, val level: Int)

/** A month name placed above the column of [weekIndex]. */
data class HeatmapMonthLabel(val weekIndex: Int, val month: Month)

/**
 * The heatmap grid: one list per week, oldest first, each with seven slots in the order of
 * [firstDayOfWeek]. A slot is `null` for days that are after today.
 */
data class Heatmap(
    val weeks: List<List<HeatmapDay?>>,
    val monthLabels: List<HeatmapMonthLabel>,
    val firstDayOfWeek: DayOfWeek,
)

/** Counts [entries] per calendar day, resolving days in [zone]. */
fun entryCountsByDay(
    entries: List<Entry>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Int> = entries.groupingBy { it.timestamp.atZone(zone).toLocalDate() }.eachCount()

/**
 * The intensity step of a day with [count] entries. The scale is absolute (one entry is level 1,
 * two are level 2, ...) until a day beats [scaleFloor], after which it stretches so that the
 * busiest day is [MAX_LEVEL]. An absolute scale matters here: most days have a single visit, and a
 * purely relative one would paint every cell with the darkest color.
 */
fun intensityLevel(count: Int, maxCount: Int, scaleFloor: Int = MAX_LEVEL): Int {
  if (count <= 0) return 0
  val scale = maxOf(maxCount, scaleFloor)
  return ((count * MAX_LEVEL + scale - 1) / scale).coerceIn(1, MAX_LEVEL)
}

/**
 * Builds the grid ending with the week of [today] and spanning [weekCount] weeks.
 *
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param firstDayOfWeek The day each column starts on, usually the locale's.
 */
fun buildHeatmap(
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    weekCount: Int = DEFAULT_WEEK_COUNT,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
): Heatmap {
  require(weekCount > 0) { "weekCount must be positive" }
  val lastWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
  val firstWeekStart = lastWeekStart.minusWeeks(weekCount - 1L)
  val maxCount =
      countsByDay.filterKeys { it >= firstWeekStart && it <= today }.values.maxOrNull() ?: 0

  val weeks =
      (0 until weekCount).map { week ->
        val weekStart = firstWeekStart.plusWeeks(week.toLong())
        (0 until 7).map { offset ->
          val date = weekStart.plusDays(offset.toLong())
          if (date.isAfter(today)) null
          else {
            val count = countsByDay[date] ?: 0
            HeatmapDay(date, count, intensityLevel(count, maxCount))
          }
        }
      }
  return Heatmap(weeks, monthLabels(firstWeekStart, weekCount), firstDayOfWeek)
}

/**
 * Labels the first column in which each month appears, skipping a label that would sit closer than
 * [minGap] columns to the previous one (it would overlap on screen).
 */
private fun monthLabels(
    firstWeekStart: LocalDate,
    weekCount: Int,
    minGap: Int = 3,
): List<HeatmapMonthLabel> {
  val labels = mutableListOf<HeatmapMonthLabel>()
  var previousMonth: Month? = null
  for (week in 0 until weekCount) {
    // A week belongs to the month in which most of it falls, i.e. the month of its fourth day.
    val month = firstWeekStart.plusWeeks(week.toLong()).plusDays(3).month
    if (month != previousMonth) {
      val lastIndex = labels.lastOrNull()?.weekIndex
      if (lastIndex == null || week - lastIndex >= minGap) {
        labels += HeatmapMonthLabel(week, month)
      }
      previousMonth = month
    }
  }
  return labels
}

/**
 * A summary of the visible [Heatmap] range.
 *
 * @property visits Entries in the range.
 * @property activeDays Days with at least one entry.
 * @property currentWeekStreak Consecutive weeks with a visit, ending this week, or last week if
 *   this one has none yet (so the streak doesn't read 0 every Monday morning).
 * @property longestWeekStreak The longest run of consecutive weeks with a visit in the range.
 */
data class HeatmapSummary(
    val visits: Int,
    val activeDays: Int,
    val currentWeekStreak: Int,
    val longestWeekStreak: Int,
)

/** Summarizes [heatmap]. Streaks are by week, since swimming every single day is unrealistic. */
fun summarize(heatmap: Heatmap): HeatmapSummary {
  val days = heatmap.weeks.flatten().filterNotNull()
  val activeWeeks = heatmap.weeks.map { week -> week.any { (it?.count ?: 0) > 0 } }

  var longest = 0
  var run = 0
  for (active in activeWeeks) {
    run = if (active) run + 1 else 0
    longest = maxOf(longest, run)
  }

  val lastIndex = activeWeeks.lastIndex
  val streakEnd = if (activeWeeks[lastIndex] || lastIndex == 0) lastIndex else lastIndex - 1
  var current = 0
  var index = streakEnd
  while (index >= 0 && activeWeeks[index]) {
    current++
    index--
  }

  return HeatmapSummary(
      visits = days.sumOf { it.count },
      activeDays = days.count { it.count > 0 },
      currentWeekStreak = current,
      longestWeekStreak = longest,
  )
}
