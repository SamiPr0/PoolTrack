package com.github.se.pooltrack.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.ui.theme.PoolTile
import com.github.se.pooltrack.ui.theme.deepWaterBrush
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Entries per week over the last [RECENT_WEEKS] weeks as bars that rise one after another when
 * shown, with the [bestWeekEntries] in a pill and, when known, the [favoriteDay] underneath.
 * Tapping a bar shows which week it is ([weekStarts] holds each bar's Monday); tapping it again
 * hides that again.
 */
@Composable
internal fun WeeklyChartCard(
    counts: List<Int>,
    weekStarts: List<LocalDate>,
    bestWeekEntries: Int,
    favoriteDay: DayOfWeek?,
    modifier: Modifier = Modifier,
) {
  var selected by rememberSaveable { mutableStateOf<Int?>(null) }
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
  PoolTile(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Last $RECENT_WEEKS weeks",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        BestWeekPill(
            value = if (bestWeekEntries == 0) "-" else "$bestWeekEntries",
            modifier = Modifier.testTag(HomeScreenTestTags.BEST_WEEK_STAT),
        )
      }
      Row(
          modifier = Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom,
      ) {
        counts.forEachIndexed { index, count ->
          val isCurrent = index == counts.lastIndex
          val rise by
              animateFloatAsState(
                  targetValue = if (shown) 1f else 0f,
                  animationSpec = tween(durationMillis = 900, delayMillis = index * 70),
                  label = "weeklyBar",
              )
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
                        .fillMaxHeight(0.8f * rise * count / maxCount)
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
      if (favoriteDay != null) {
        FavoriteDayRow(
            day = favoriteDay,
            modifier =
                Modifier.padding(top = 12.dp).testTag(HomeScreenTestTags.FAVORITE_DAY_BANNER),
        )
      }
    }
  }
}

/** The most entries in one week, in a small pill: "Best week · 4". */
@Composable
private fun BestWeekPill(value: String, modifier: Modifier = Modifier) {
  Row(
      modifier =
          modifier
              .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
              .padding(horizontal = 10.dp, vertical = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Text(text = "Best week", style = style, color = color)
    Text(text = "·", style = style, color = color)
    Text(text = value, style = style, color = color, fontWeight = FontWeight.Bold)
  }
}

/** The weekday the user swims on most, under a fine line: "You usually swim on Tuesday". */
@Composable
private fun FavoriteDayRow(day: DayOfWeek, modifier: Modifier = Modifier) {
  val dayName = day.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
  Column(modifier = modifier.fillMaxWidth()) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Outlined.Favorite,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
      )
      Text(
          text = "You usually swim on $dayName",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 8.dp),
      )
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
