package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.ui.history.heatmap.visitsLabel
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.SectionHeader
import com.github.se.pooltrack.ui.theme.deepWaterBrush
import com.github.se.pooltrack.ui.theme.poolTileColor
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

/** The sticky title above the entries of one week, with its totals and a lane rope. */
@Composable
internal fun WeekHeader(
    label: String,
    visits: Int,
    meters: Int,
    loggedVisits: Int,
    modifier: Modifier = Modifier,
) {
  // Opaque, since the header sticks on top of the entries scrolling under it.
  Surface(
      modifier = modifier.fillMaxWidth().testTag(HistoryScreenTestTags.WEEK_HEADER),
      color = MaterialTheme.colorScheme.background,
  ) {
    SectionHeader(
        title = label,
        detail = weekTotals(visits, meters, loggedVisits),
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
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
                  if (isToday) deepWaterBrush(scheme)
                  else SolidColor(scheme.surfaceVariant.copy(alpha = 0.5f)),
                  RoundedCornerShape(10.dp),
              )
              .padding(vertical = 6.dp),
  ) {
    Text(
        text = day.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale),
        style = MaterialTheme.typography.labelSmall,
        color = if (isToday) PoolPalette.OnDeepVariant else scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Text(
        text = day.dayOfMonth.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isToday) PoolPalette.OnDeep else scheme.onSurface,
    )
  }
}

/**
 * Whether the entry at [index] of [rows] opens its week ([first]) and closes it ([last]): the
 * entries of a week share one tile, rounded only at its two ends.
 */
internal data class EntryEdges(val first: Boolean, val last: Boolean)

/** The [EntryEdges] of the entry at [index] of [rows], from the rows around it. */
internal fun entryEdges(rows: List<HistoryRow>, index: Int): EntryEdges =
    EntryEdges(
        first = index == 0 || rows[index - 1] is HistoryRow.Week,
        last = index == rows.lastIndex || rows[index + 1] is HistoryRow.Week,
    )

/**
 * A flat, compact row for one entry: the day it happened on, then its time and swim details. Tap it
 * to open the entry. The rows of a week share one tile: the row rounds the tile's corners at the
 * [edges] it is on, and draws a fine line under itself unless it closes the week. Deleting is done
 * from the entry's own screen, never from the list, so scrolling can't delete a visit by accident.
 *
 * @param day The day of the entry, shown in the date block.
 * @param isToday Whether [day] is today.
 * @param texts What the row says, see [entryRowTexts].
 * @param highlighted Whether to tint the row, e.g. because its day was picked in the calendar.
 * @param edges Where the row sits in its week's tile.
 * @param onClick Called when the row is tapped.
 */
@Composable
internal fun EntryRow(
    day: LocalDate,
    isToday: Boolean,
    texts: EntryRowTexts,
    highlighted: Boolean,
    edges: EntryEdges,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val corner = 16.dp
  Surface(
      onClick = onClick,
      modifier = modifier.testTag(HistoryScreenTestTags.ENTRY_ITEM),
      shape =
          RoundedCornerShape(
              topStart = if (edges.first) corner else 0.dp,
              topEnd = if (edges.first) corner else 0.dp,
              bottomStart = if (edges.last) corner else 0.dp,
              bottomEnd = if (edges.last) corner else 0.dp,
          ),
      color =
          if (highlighted) MaterialTheme.colorScheme.primaryContainer
          else poolTileColor(MaterialTheme.colorScheme),
  ) {
    Column {
      Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
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
      }
      if (!edges.last) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
      }
    }
  }
}

/**
 * "5 visits · 4.2 km". The distance is left out when none was logged that week, and reads "logged"
 * (as in "4.2 km logged") when only some of the visits have one, since the total then undercounts.
 */
internal fun weekTotals(visits: Int, meters: Int, loggedVisits: Int): String {
  val distance =
      if (meters > 0) {
        formatTotalDistance(meters) + if (loggedVisits < visits) " logged" else ""
      } else null
  return listOfNotNull(visitsLabel(visits), distance).joinToString(" · ")
}
