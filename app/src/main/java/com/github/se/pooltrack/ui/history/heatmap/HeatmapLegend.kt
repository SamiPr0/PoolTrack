package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * How opaque a day with one visit is. High, because most days have at most one visit: a faint fill
 * would make every visited day look like it barely counted.
 */
const val DAY_MIN_ALPHA = 0.7f

/**
 * How opaque the lightest tile of the year view is; those span 1 to 12 visits, so it starts low.
 */
const val YEAR_MIN_ALPHA = 0.25f

/** The opacity of the primary color at [level] (1 to [MAX_LEVEL]), from [minAlpha] up to 1. */
fun levelAlpha(level: Int, minAlpha: Float): Float =
    minAlpha + (1f - minAlpha) * (level.coerceIn(1, MAX_LEVEL) - 1) / (MAX_LEVEL - 1)

/**
 * The fill at [level], from nothing (0) to the busiest ([MAX_LEVEL]); the lightest filled level has
 * the opacity [minAlpha].
 */
@Composable
fun heatmapColor(level: Int, minAlpha: Float = DAY_MIN_ALPHA): Color =
    if (level <= 0) Color.Transparent
    else MaterialTheme.colorScheme.primary.copy(alpha = levelAlpha(level, minAlpha))

/** The "Less, five dots, More" key explaining the colors. */
@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = "Less",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    (0..MAX_LEVEL).forEach { level ->
      Box(
          modifier =
              Modifier.padding(start = 4.dp)
                  .size(12.dp)
                  .background(
                      if (level == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                      else heatmapColor(level),
                      CircleShape,
                  )
      )
    }
    Text(
        text = "More",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
  }
}
