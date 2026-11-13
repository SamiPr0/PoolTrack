package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.ui.theme.deepWaterBrush
import com.github.se.pooltrack.ui.theme.poolTileColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A small bar chart of entries per week over the last [RECENT_WEEKS] weeks. Tapping a bar shows
 * which week it is ([weekStarts] holds each bar's Monday); tapping it again hides that again.
 */
@Composable
internal fun WeeklyChartCard(
    counts: List<Int>,
    weekStarts: List<LocalDate>,
    modifier: Modifier = Modifier,
) {
  var selected by rememberSaveable { mutableStateOf<Int?>(null) }
  val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
  Card(modifier = modifier.fillMaxWidth(), colors = poolTileColors()) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(
          text = "Last $RECENT_WEEKS weeks",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
      )
      Row(
          modifier = Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom,
      ) {
        counts.forEachIndexed { index, count ->
          val isCurrent = index == counts.lastIndex
          Column(
              modifier =
                  Modifier.weight(1f)
                      .fillMaxHeight()
                      .testTag(HomeScreenTestTags.WEEKLY_CHART_BAR)
                      .clickable(enabled = index in weekStarts.indices) {
                        selected = if (selected == index) null else index
                      },
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
                text = if (count > 0) "$count" else "",
                style = MaterialTheme.typography.labelSmall,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(0.8f * count / maxCount)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isCurrent || index == selected) {
                              deepWaterBrush(MaterialTheme.colorScheme)
                            } else {
                              SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            }
                        )
            )
          }
        }
      }
      val selectedIndex = selected
      if (selectedIndex != null && selectedIndex in weekStarts.indices) {
        Text(
            text = weekLegend(weekStarts[selectedIndex], counts[selectedIndex]),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp).testTag(HomeScreenTestTags.WEEKLY_CHART_LEGEND),
        )
      }
    }
  }
}

/** "12 Oct - 18 Oct: 3 entries" for the week starting on [weekStart] (a Monday). */
internal fun weekLegend(
    weekStart: LocalDate,
    count: Int,
    locale: Locale = Locale.getDefault(),
): String {
  val format = DateTimeFormatter.ofPattern("d MMM", locale)
  val range = "${weekStart.format(format)} - ${weekStart.plusDays(6).format(format)}"
  return "$range: ${if (count == 1) "1 entry" else "$count entries"}"
}
