package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object HistoryOverviewTestTags {
  const val CARD = "HistoryOverviewCard"
  const val TOGGLE = "HistoryOverviewToggle"
}

/**
 * The card on top of the History list. By default it is compact: just the current week as seven
 * days. The bar along its bottom expands it to the month calendar, which zooms out to the year.
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
  val months =
      remember(countsByDay, today) {
        monthsBetween(YearMonth.from(countsByDay.keys.minOrNull() ?: today), YearMonth.from(today))
      }

  Card(modifier = modifier.fillMaxWidth().testTag(HistoryOverviewTestTags.CARD)) {
    Column {
      Column(
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
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
      // A bar across the whole card, so expanding it doesn't depend on hitting a small button.
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clickable { expanded = !expanded }
                  .padding(vertical = 12.dp)
                  .testTag(HistoryOverviewTestTags.TOGGLE),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = if (expanded) "Hide calendar" else "Show calendar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector =
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
