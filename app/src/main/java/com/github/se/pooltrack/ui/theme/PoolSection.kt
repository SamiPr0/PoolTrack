package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The title of a section, then a lane rope across the rest of the line: the one divider between a
 * screen's sections. [detail], when given, follows the title in a quieter color, e.g. a week's
 * totals.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, detail: String? = null) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    if (detail != null) {
      Text(
          text = detail,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    LaneRope(modifier = Modifier.weight(1f))
  }
}
