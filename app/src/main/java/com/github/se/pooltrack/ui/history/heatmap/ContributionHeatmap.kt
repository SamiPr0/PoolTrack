package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object ContributionHeatmapTestTags {
  const val HEATMAP = "ContributionHeatmap"

  fun cell(date: LocalDate) = "ContributionHeatmapCell_$date"
}

private val CellSize = 14.dp
private val CellGap = 3.dp
private val CellShape = RoundedCornerShape(3.dp)
private val WeekdayLabelWidth = 32.dp
private val MonthLabelHeight = 16.dp

/** The fill of a cell at [level], from an empty slot (0) to the busiest days ([MAX_LEVEL]). */
@Composable
fun heatmapColor(level: Int): Color {
  val scheme = MaterialTheme.colorScheme
  return when (level) {
    0 -> scheme.onSurface.copy(alpha = 0.08f)
    1 -> scheme.primary.copy(alpha = 0.3f)
    2 -> scheme.primary.copy(alpha = 0.5f)
    3 -> scheme.primary.copy(alpha = 0.75f)
    else -> scheme.primary
  }
}

/**
 * A calendar of squares in the style of GitHub's contribution graph: one column per week, one row
 * per weekday, darker for more entries. It scrolls horizontally and starts at the current week.
 *
 * @param heatmap The grid to draw, see [buildHeatmap].
 * @param selectedDate The highlighted day, if any.
 * @param onDayClick Called with the tapped day.
 * @param locale The locale for month and weekday names.
 */
@Composable
fun ContributionHeatmap(
    heatmap: Heatmap,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
) {
  val scrollState = rememberScrollState()
  LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }
  val step = CellSize + CellGap

  Row(modifier = modifier.testTag(ContributionHeatmapTestTags.HEATMAP)) {
    // Only every other row is named, otherwise the labels would not fit between the squares.
    Column(
        modifier = Modifier.padding(top = MonthLabelHeight).width(WeekdayLabelWidth),
        verticalArrangement = Arrangement.spacedBy(CellGap),
    ) {
      (0 until 7).forEach { row ->
        Box(modifier = Modifier.height(CellSize), contentAlignment = Alignment.CenterStart) {
          if (row % 2 == 1) {
            Text(
                text =
                    heatmap.firstDayOfWeek
                        .plus(row.toLong())
                        .getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
          }
        }
      }
    }
    Column(modifier = Modifier.horizontalScroll(scrollState)) {
      Box(modifier = Modifier.height(MonthLabelHeight).width(step * heatmap.weeks.size)) {
        heatmap.monthLabels.forEach { label ->
          Text(
              text = label.month.getDisplayName(TextStyle.SHORT, locale),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              modifier = Modifier.offset(x = step * label.weekIndex),
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
        heatmap.weeks.forEach { week ->
          Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            week.forEach { day ->
              if (day == null) {
                Box(modifier = Modifier.size(CellSize))
              } else {
                HeatmapCell(
                    day = day,
                    selected = day.date == selectedDate,
                    onClick = { onDayClick(day.date) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeatmapCell(day: HeatmapDay, selected: Boolean, onClick: () -> Unit) {
  val description =
      when (day.count) {
        0 -> "${day.date}: no entries"
        1 -> "${day.date}: 1 entry"
        else -> "${day.date}: ${day.count} entries"
      }
  Box(
      modifier =
          Modifier.size(CellSize)
              .background(heatmapColor(day.level), CellShape)
              .then(
                  if (selected) {
                    Modifier.border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                        CellShape,
                    )
                  } else Modifier
              )
              .clickable(onClick = onClick)
              .semantics { contentDescription = description }
              .testTag(ContributionHeatmapTestTags.cell(day.date))
  )
}

/** The "Less, five squares, More" key explaining the colors. */
@Composable
fun HeatmapLegend(modifier: Modifier = Modifier, cellSize: Dp = 12.dp) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = "Less",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    (0..MAX_LEVEL).forEach { level ->
      Box(
          modifier =
              Modifier.padding(start = CellGap)
                  .size(cellSize)
                  .background(heatmapColor(level), CellShape)
      )
    }
    Text(
        text = "More",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = CellGap * 2),
    )
  }
}
