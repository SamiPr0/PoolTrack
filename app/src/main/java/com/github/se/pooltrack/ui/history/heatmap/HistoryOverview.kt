package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

object HistoryOverviewTestTags {
  const val CARD = "HistoryOverviewCard"
  const val SUMMARY = "HistoryOverviewSummary"
  const val CLEAR_FILTER = "HistoryOverviewClearFilter"
}

/** "34 visits in the last year" for [summary]. */
fun summaryTitle(summary: HeatmapSummary): String =
    if (summary.visits == 1) "1 visit in the last year"
    else "${summary.visits} visits in the last year"

/** The week-streak sentence, or `null` when there is no streak worth mentioning. */
fun streakText(summary: HeatmapSummary): String? =
    when {
      summary.currentWeekStreak >= 2 ->
          "${summary.currentWeekStreak}-week streak (best: ${summary.longestWeekStreak})"
      summary.longestWeekStreak >= 2 -> "Best streak: ${summary.longestWeekStreak} weeks in a row"
      else -> null
    }

/**
 * The card on top of the History list: how much the user swam over the last year as a heatmap, a
 * one-line summary, and, once a day is picked, a bar to clear that filter.
 *
 * @param heatmap The grid, see [buildHeatmap].
 * @param selectedDate The day the list is filtered to, if any.
 * @param onDayClick Called when a day is tapped; the caller toggles the filter.
 * @param onClearSelection Called to show every day again.
 * @param selectedLabel How to name the selected day, e.g. "Monday, Oct 5".
 */
@Composable
fun HistoryOverview(
    heatmap: Heatmap,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    selectedLabel: (LocalDate) -> String = { it.toString() },
) {
  val summary = summarize(heatmap)
  val activeDays = "${summary.activeDays} active ${if (summary.activeDays == 1) "day" else "days"}"
  Card(modifier = modifier.fillMaxWidth().testTag(HistoryOverviewTestTags.CARD)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(modifier = Modifier.testTag(HistoryOverviewTestTags.SUMMARY)) {
        Text(
            text = summaryTitle(summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = listOfNotNull(activeDays, streakText(summary)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      ContributionHeatmap(
          heatmap = heatmap,
          selectedDate = selectedDate,
          onDayClick = onDayClick,
      )
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (selectedDate != null) {
          Text(
              text = "Showing ${selectedLabel(selectedDate)}",
              style = MaterialTheme.typography.labelMedium,
          )
          TextButton(
              onClick = onClearSelection,
              modifier = Modifier.testTag(HistoryOverviewTestTags.CLEAR_FILTER),
          ) {
            Text("Show all")
          }
        } else {
          Text(
              text = "Tap a day to filter",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HeatmapLegend()
        }
      }
    }
  }
}
