package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object HistoryOverviewTestTags {
  const val CARD = "HistoryOverviewCard"
  const val SUMMARY = "HistoryOverviewSummary"
  const val TOGGLE = "HistoryOverviewToggle"
}

/** The week-streak sentence ("4-week streak"), or `null` when there is no streak yet. */
fun streakText(summary: HeatmapSummary): String? =
    if (summary.currentWeekStreak >= 2) "${summary.currentWeekStreak}-week streak" else null

/** The one-line summary: "5 this week · 4-week streak". */
fun overviewSummary(visitsThisWeek: Int, summary: HeatmapSummary): String =
    listOfNotNull("$visitsThisWeek this week", streakText(summary)).joinToString(" · ")

/**
 * The card on top of the History list. By default it is compact: a one-line summary and the current
 * week as seven days. Expanding it shows the month calendar, which zooms out to the year.
 *
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param today The current day.
 * @param firstDayOfWeek The weekday the week strip and calendar columns start on.
 * @param selectedDate The day to highlight, if any.
 * @param onDayClick Called when a day is tapped.
 */
@Composable
fun HistoryOverview(
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val summary =
      remember(countsByDay, today, firstDayOfWeek) {
        summarize(buildHeatmap(countsByDay, today, firstDayOfWeek = firstDayOfWeek))
      }
  val visitsThisWeek =
      remember(countsByDay, today, firstDayOfWeek) {
        buildWeekStrip(today, countsByDay, firstDayOfWeek).sumOf { it.count }
      }
  val months =
      remember(countsByDay, today) {
        monthsBetween(YearMonth.from(countsByDay.keys.minOrNull() ?: today), YearMonth.from(today))
      }

  Card(modifier = modifier.fillMaxWidth().testTag(HistoryOverviewTestTags.CARD)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = overviewSummary(visitsThisWeek, summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).testTag(HistoryOverviewTestTags.SUMMARY),
        )
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag(HistoryOverviewTestTags.TOGGLE),
        ) {
          Text(if (expanded) "Hide" else "Calendar")
          Icon(
              imageVector =
                  if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
              contentDescription = null,
          )
        }
      }
      if (expanded) {
        HistoryCalendar(
            months = months,
            countsByDay = countsByDay,
            today = today,
            firstDayOfWeek = firstDayOfWeek,
            selectedDate = selectedDate,
            onDayClick = onDayClick,
        )
        HeatmapLegend(modifier = Modifier.align(Alignment.End))
      } else {
        WeekStrip(
            countsByDay = countsByDay,
            today = today,
            selectedDate = selectedDate,
            onDayClick = onDayClick,
            firstDayOfWeek = firstDayOfWeek,
        )
      }
    }
  }
}
