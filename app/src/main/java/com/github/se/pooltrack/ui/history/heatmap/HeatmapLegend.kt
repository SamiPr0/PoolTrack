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

/** The fill of a day at [level], from no entry (0) to the busiest days ([MAX_LEVEL]). */
@Composable
fun heatmapColor(level: Int): Color {
  val scheme = MaterialTheme.colorScheme
  return when (level) {
    0 -> Color.Transparent
    1 -> scheme.primary.copy(alpha = 0.3f)
    2 -> scheme.primary.copy(alpha = 0.5f)
    3 -> scheme.primary.copy(alpha = 0.75f)
    else -> scheme.primary
  }
}

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
