package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object WeekStripTestTags {
  const val STRIP = "WeekStrip"

  fun day(date: LocalDate) = "WeekStripDay_$date"
}

/**
 * The current week as one row of seven round days, filled for the days the user went. It is the
 * compact stand-in for the month calendar: one glance answers "did I swim this week?".
 *
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param today The current day, marked with a ring; later days can't be tapped.
 * @param selectedDate The highlighted day, if any.
 * @param onDayClick Called with the tapped day.
 */
@Composable
fun WeekStrip(
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    firstDayOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
) {
  val days =
      remember(countsByDay, today, firstDayOfWeek) {
        buildWeekStrip(today, countsByDay, firstDayOfWeek)
      }
  Row(modifier = modifier.fillMaxWidth().testTag(WeekStripTestTags.STRIP)) {
    days.forEach { day ->
      Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(3.dp)) {
          DayCell(
              day = day,
              selected = day.date == selectedDate,
              testTag = WeekStripTestTags.day(day.date),
              onClick = { onDayClick(day.date) },
          )
        }
      }
    }
  }
}
