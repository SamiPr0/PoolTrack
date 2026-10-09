package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.ui.history.heatmap.visitsLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** The first day of the week that contains [day], where weeks start on [firstDayOfWeek]. */
internal fun weekStart(day: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
    day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

/**
 * Names the week starting on [weekStart]: "This week", "Last week", or its dates, e.g. "Oct 5 – 11"
 * or "Sep 28 – Oct 4". The year is added when the week isn't in [today]'s year (and both years when
 * it spans New Year's Eve).
 */
internal fun weekLabel(
    weekStart: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    locale: Locale = Locale.getDefault(),
): String {
  val thisWeek = weekStart(today, firstDayOfWeek)
  if (weekStart == thisWeek) return "This week"
  if (weekStart == thisWeek.minusWeeks(1)) return "Last week"

  val end = weekStart.plusDays(6)
  fun format(date: LocalDate, pattern: String) =
      date.format(DateTimeFormatter.ofPattern(pattern, locale))
  return when {
    weekStart.year != end.year ->
        "${format(weekStart, "MMM d, yyyy")} – ${format(end, "MMM d, yyyy")}"
    else -> {
      val range =
          if (weekStart.month == end.month) "${format(weekStart, "MMM d")} – ${end.dayOfMonth}"
          else "${format(weekStart, "MMM d")} – ${format(end, "MMM d")}"
      if (weekStart.year == today.year) range else "$range, ${weekStart.year}"
    }
  }
}

/** The sticky title above the entries of one week, with how many visits it had. */
@Composable
internal fun WeekHeader(label: String, visits: Int, meters: Int, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(HistoryScreenTestTags.WEEK_HEADER),
      color = MaterialTheme.colorScheme.background,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
      )
      Text(
          text = weekTotals(visits, meters),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** The weekday and day of the month, stacked ("THU" over "8"); today's is tinted. */
@Composable
private fun DateBlock(day: LocalDate, isToday: Boolean, locale: Locale = Locale.getDefault()) {
  val scheme = MaterialTheme.colorScheme
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.width(48.dp)
              .background(
                  if (isToday) scheme.primaryContainer
                  else scheme.surfaceVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(10.dp),
              )
              .padding(vertical = 6.dp),
  ) {
    Text(
        text = day.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale),
        style = MaterialTheme.typography.labelSmall,
        color = if (isToday) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Text(
        text = day.dayOfMonth.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isToday) scheme.onPrimaryContainer else scheme.onSurface,
    )
  }
}

/**
 * A flat, compact row for one entry: the day it happened on, then its time and swim details. Tap it
 * to open the entry; swipe it to the left to delete it (the caller offers an Undo). Screen readers
 * get a "Delete entry" action instead of the swipe.
 *
 * @param day The day of the entry, shown in the date block.
 * @param isToday Whether [day] is today.
 * @param texts What the row says, see [entryRowTexts].
 * @param highlighted Whether to tint the row, e.g. because its day was picked in the calendar.
 * @param onClick Called when the row is tapped.
 * @param onDelete Called once the row was swiped away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableEntryRow(
    day: LocalDate,
    isToday: Boolean,
    texts: EntryRowTexts,
    highlighted: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val dismissState =
      rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
              onDelete()
              true
            } else false
          }
      )
  SwipeToDismissBox(
      state = dismissState,
      enableDismissFromStartToEnd = false,
      modifier =
          modifier.testTag(HistoryScreenTestTags.ENTRY_ITEM).semantics {
            customActions =
                listOf(
                    CustomAccessibilityAction("Delete entry") {
                      onDelete()
                      true
                    }
                )
          },
      backgroundContent = {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd,
        ) {
          Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onError,
              modifier = Modifier.padding(end = 20.dp),
          )
        }
      },
  ) {
    Surface(
        onClick = onClick,
        color =
            if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.background,
    ) {
      Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          DateBlock(day = day, isToday = isToday)
          Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                text = texts.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            texts.secondary?.let {
              Text(
                  text = it,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
    }
  }
}

/** "5 visits · 4.2 km"; the distance is left out when none was logged that week. */
internal fun weekTotals(visits: Int, meters: Int): String =
    listOfNotNull(visitsLabel(visits), if (meters > 0) formatTotalDistance(meters) else null)
        .joinToString(" · ")
