package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

object MonthCalendarTestTags {
  const val CALENDAR = "MonthCalendar"
  const val TITLE = "MonthCalendarTitle"
  const val PREVIOUS = "MonthCalendarPrevious"
  const val NEXT = "MonthCalendarNext"

  fun day(date: LocalDate) = "MonthCalendarDay_$date"
}

/** A calendar page always has this many rows, so swiping never changes the height. */
private const val ROWS = 6

/**
 * A month calendar in the spirit of the iPhone's: a title with previous/next buttons, a row of
 * weekday initials and a grid of round days that fills the width. Days with entries are filled,
 * darker for more entries. Swipe sideways or use the buttons to change month.
 *
 * @param months The months that can be shown, oldest first; it opens on the last one.
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param today The current day, marked with a ring; later days can't be selected.
 * @param selectedDate The highlighted day, if any.
 * @param onDayClick Called with the tapped day.
 * @param firstDayOfWeek The weekday the columns start on, usually the locale's.
 */
@Composable
fun MonthCalendar(
    months: List<YearMonth>,
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    locale: Locale = Locale.getDefault(),
) {
  require(months.isNotEmpty()) { "months must not be empty" }
  val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }
  val scope = rememberCoroutineScope()
  val maxCount = remember(countsByDay) { countsByDay.values.maxOrNull() ?: 0 }
  val current = months[pagerState.currentPage]

  Column(modifier = modifier.fillMaxWidth().testTag(MonthCalendarTestTags.CALENDAR)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text =
                "${current.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${current.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(MonthCalendarTestTags.TITLE),
        )
        val visits = countsByDay.filterKeys { YearMonth.from(it) == current }.values.sum()
        Text(
            text =
                when (visits) {
                  0 -> "No visits"
                  1 -> "1 visit"
                  else -> "$visits visits"
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(
          onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
          enabled = pagerState.currentPage > 0,
          modifier = Modifier.testTag(MonthCalendarTestTags.PREVIOUS),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
      }
      IconButton(
          onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
          enabled = pagerState.currentPage < months.lastIndex,
          modifier = Modifier.testTag(MonthCalendarTestTags.NEXT),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
      }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
      (0 until 7).forEach { offset ->
        Text(
            text = firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
      }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
      val grid =
          remember(months[page], countsByDay, today, firstDayOfWeek, maxCount) {
            buildMonthGrid(months[page], countsByDay, today, firstDayOfWeek, maxCount)
          }
      Column(modifier = Modifier.fillMaxWidth()) {
        (0 until ROWS).forEach { row ->
          Row(modifier = Modifier.fillMaxWidth()) {
            val week = grid.weeks.getOrNull(row)
            (0 until 7).forEach { column ->
              val day = week?.get(column)
              Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                if (day != null) {
                  DayCell(day, selected = day.date == selectedDate) { onDayClick(day.date) }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DayCell(day: MonthDay, selected: Boolean, onClick: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val textColor =
      when {
        day.isFuture -> scheme.onSurface.copy(alpha = 0.3f)
        day.level >= 3 -> scheme.onPrimary
        else -> scheme.onSurface
      }
  val ring =
      when {
        selected -> BorderStroke(2.dp, scheme.onSurface)
        day.isToday -> BorderStroke(1.5.dp, scheme.primary)
        else -> null
      }
  val description =
      when (day.count) {
        0 -> "${day.date}: no entries"
        1 -> "${day.date}: 1 entry"
        else -> "${day.date}: ${day.count} entries"
      }
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(1f)
              .background(heatmapColor(day.level), CircleShape)
              .then(if (ring != null) Modifier.border(ring, CircleShape) else Modifier)
              .then(if (day.isFuture) Modifier else Modifier.clickable(onClick = onClick))
              .semantics { contentDescription = description }
              .testTag(MonthCalendarTestTags.day(day.date)),
  ) {
    Text(
        text = day.date.dayOfMonth.toString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
        color = textColor,
    )
  }
}
