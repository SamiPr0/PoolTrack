package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** How far the calendar is zoomed: one month of days, or a year of months. */
enum class CalendarZoom {
  Month,
  Year,
}

/**
 * The calendar with two zoom levels. It opens on the current month; zoom out (pinch, or the "<
 * year" button) for the year's months shaded by how often the user went, and tap a month, or pinch
 * in, to come back.
 *
 * @param months The months that can be shown, oldest first.
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param today The current day.
 * @param selectedDate The highlighted day, if any.
 * @param onDayClick Called with the tapped day.
 * @param firstDayOfWeek The weekday the month columns start on.
 */
@Composable
fun HistoryCalendar(
    months: List<YearMonth>,
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    firstDayOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
) {
  var zoom by remember { mutableStateOf(CalendarZoom.Month) }
  var focusedMonth by remember { mutableStateOf(months.last()) }
  val years = remember(months) { yearsBetween(months.first().year, months.last().year) }

  Crossfade(
      targetState = zoom,
      label = "calendar zoom",
      modifier =
          modifier
              .fillMaxWidth()
              .pinchToZoom(
                  onZoomOut = { zoom = CalendarZoom.Year },
                  onZoomIn = { zoom = CalendarZoom.Month },
              ),
  ) { level ->
    when (level) {
      CalendarZoom.Month ->
          MonthCalendar(
              months = months,
              countsByDay = countsByDay,
              today = today,
              selectedDate = selectedDate,
              onDayClick = onDayClick,
              firstDayOfWeek = firstDayOfWeek,
              initialMonth = focusedMonth,
              onMonthChanged = { focusedMonth = it },
              onZoomOut = { current ->
                focusedMonth = current
                zoom = CalendarZoom.Year
              },
          )
      CalendarZoom.Year ->
          YearCalendar(
              years = years,
              countsByDay = countsByDay,
              today = today,
              initialYear = focusedMonth.year,
              onYearChanged = { year -> focusedMonth = monthToFocus(focusedMonth, year, months) },
              onMonthClick = { month ->
                focusedMonth = month
                zoom = CalendarZoom.Month
              },
          )
    }
  }
}
