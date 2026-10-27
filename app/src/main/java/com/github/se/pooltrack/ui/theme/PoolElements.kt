package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/*
 * The pool's own furniture: lane ropes with their floats, the dark lines on the pool floor, and
 * water columns with a wavy top. Plain shapes again, nothing animated.
 */

/** The colors of a lane rope's floats. */
enum class FloatColor {
  RED,
  WHITE,
  BLUE,
}

/** How many floats at each end of a lane rope are red, warning swimmers that the wall is near. */
const val RED_FLOATS_AT_WALLS = 3

/**
 * The color of float [index] out of [count] on a lane rope: red near both walls, then blue and
 * white alternating in between, like a racing lane rope.
 */
fun laneFloatColor(index: Int, count: Int): FloatColor =
    when {
      index < RED_FLOATS_AT_WALLS || index >= count - RED_FLOATS_AT_WALLS -> FloatColor.RED
      index % 2 == 0 -> FloatColor.BLUE
      else -> FloatColor.WHITE
    }

/**
 * Which floats of an entry counter are still available, oldest entries first: `true` for each of
 * the [maxEntries] entries not used yet, `false` for the [usedCount] used ones. At most
 * [MAX_ENTRY_FLOATS] floats are drawn; past that, the counter is left out.
 */
fun entryFloats(usedCount: Int, maxEntries: Int): List<Boolean> {
  if (maxEntries <= 0 || maxEntries > MAX_ENTRY_FLOATS) return emptyList()
  val used = usedCount.coerceIn(0, maxEntries)
  return List(maxEntries) { it >= used }
}

/** The most floats an entry counter shows; bigger passes are only shown as text. */
const val MAX_ENTRY_FLOATS = 30

private val RopeRed = Color(0xFFE5484D)
private val RopeWhite = Color(0xFFF2F6F8)
private val RopeBlue = Color(0xFF2E86DE)

private fun FloatColor.toColor(): Color =
    when (this) {
      FloatColor.RED -> RopeRed
      FloatColor.WHITE -> RopeWhite
      FloatColor.BLUE -> RopeBlue
    }

/**
 * A lane rope across the available width: a thin cord threaded through capsule-shaped floats, red
 * near the ends and blue and white in between (see [laneFloatColor]).
 */
@Composable
fun LaneRope(modifier: Modifier = Modifier, height: Dp = 10.dp) {
  val cord = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
  Canvas(modifier = modifier.fillMaxWidth().height(height)) {
    val floatWidth = size.height * 1.6f
    val gap = size.height * 0.35f
    val count = ((size.width + gap) / (floatWidth + gap)).toInt().coerceAtLeast(1)
    // Centre the row of floats so both ends look the same.
    val used = count * floatWidth + (count - 1) * gap
    val start = (size.width - used) / 2f
    drawLine(
        cord,
        Offset(0f, size.height / 2f),
        Offset(size.width, size.height / 2f),
        strokeWidth = 1.5.dp.toPx(),
    )
    repeat(count) { i ->
      drawFloat(
          left = start + i * (floatWidth + gap),
          width = floatWidth,
          color = laneFloatColor(i, count).toColor(),
          filled = true,
      )
    }
  }
}

/**
 * A pass's entries as the floats of a lane rope: a solid float for each entry still available, a
 * hollow one for each used. Draws nothing for passes bigger than [MAX_ENTRY_FLOATS].
 */
@Composable
fun EntryFloats(
    usedCount: Int,
    maxEntries: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
  val floats = entryFloats(usedCount, maxEntries)
  if (floats.isEmpty()) return
  Canvas(modifier = modifier.fillMaxWidth().height(12.dp)) {
    val gap = 3.dp.toPx()
    val floatWidth = (size.width - gap * (floats.size - 1)) / floats.size
    floats.forEachIndexed { i, available ->
      drawFloat(
          left = i * (floatWidth + gap),
          width = floatWidth,
          color = if (available) color else color.copy(alpha = 0.5f),
          filled = available,
      )
    }
  }
}

private fun DrawScope.drawFloat(left: Float, width: Float, color: Color, filled: Boolean) {
  val radius = CornerRadius(size.height / 2f)
  if (filled) {
    drawRoundRect(color, Offset(left, 0f), Size(width, size.height), radius)
  } else {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color,
        Offset(left + stroke / 2f, stroke / 2f),
        Size(width - stroke, size.height - stroke),
        radius,
        style = Stroke(stroke),
    )
  }
}

/**
 * The dark lane lines painted on a pool floor, seen through the water: [lanes] horizontal stripes
 * across the space, each ending in the "T" that tells swimmers the wall is coming.
 */
fun Modifier.poolFloorLines(color: Color, lanes: Int = 2): Modifier = drawBehind {
  val stripe = 6.dp.toPx()
  val margin = 28.dp.toPx()
  val tHeight = 22.dp.toPx()
  for (lane in 1..lanes) {
    val y = size.height * lane / (lanes + 1)
    drawRect(color, Offset(margin, y - stripe / 2f), Size(size.width - margin * 2, stripe))
    // The T bars at both ends.
    drawRect(color, Offset(margin - stripe, y - tHeight / 2f), Size(stripe, tHeight))
    drawRect(color, Offset(size.width - margin, y - tHeight / 2f), Size(stripe, tHeight))
  }
}

/** A column of water: square at the bottom, its top a small wave. Used for the weekly bars. */
class WaterColumnShape(
    private val amplitude: Dp = 3.dp,
    private val wavelength: Dp = 16.dp,
) : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
      Outline.Generic(
          waterPath(
              size,
              level = 1f,
              amplitude = with(density) { amplitude.toPx() },
              wavelength = with(density) { wavelength.toPx() },
          )
      )
}
