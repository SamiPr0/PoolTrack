package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A card laid on the pool: [poolTileColors] with a fine edge, so every card of a screen reads as
 * the same kind of tile.
 */
@Composable
fun PoolTile(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
  Card(
      modifier = modifier,
      shape = shape,
      colors = poolTileColors(),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
      content = content,
  )
}
