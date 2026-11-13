package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// The floor and walls of the pool: tiles, depth marks and the lane line.

/**
 * Paints the pool floor behind a screen: [scheme]'s page color, a grid of tiles, and a gradient
 * that gets slightly deeper toward the bottom, like the water above the tiles.
 */
fun Modifier.poolTiles(scheme: ColorScheme): Modifier = drawBehind {
  drawRect(scheme.background)
  val tile = TILE_SIZE.toPx()
  val line = scheme.primary.copy(alpha = TILE_LINE_ALPHA)
  var x = 0f
  while (x <= size.width) {
    drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    x += tile
  }
  var y = 0f
  while (y <= size.height) {
    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    y += tile
  }
  drawRect(
      Brush.verticalGradient(
          0f to Color.Transparent,
          0.55f to scheme.primary.copy(alpha = 0.06f),
          1f to scheme.primary.copy(alpha = 0.16f),
      )
  )
}

/** The side of one tile of the floor. */
private val TILE_SIZE = 24.dp
/** How strongly the lines between the tiles show. */
private const val TILE_LINE_ALPHA = 0.07f

/** The depths marked along the wall, from the shallow end to the deep end, in metres. */
val PoolDepths = listOf(0.8, 1.2, 1.5, 1.8, 2.2)

/** "1.5 m": how a depth is written on the wall. */
fun depthLabel(meters: Double): String = String.format(Locale.ROOT, "%.1f m", meters)

/**
 * The color of the deepest depth mark: the light theme's deepest water, and in the dark theme its
 * lightest step, since the darkest one would vanish into the dark page.
 */
fun deepestMarkColor(scheme: ColorScheme): Color =
    if (isDarkScheme(scheme)) PoolPalette.DeepDark.first() else PoolPalette.DeepLight.last()

/**
 * The depth marks painted along a pool's wall, drawn down the right edge of a screen: a line that
 * darkens with depth, a tick for each of [PoolDepths] and its label written vertically next to it.
 * It is meant to sit behind the content, in a narrow strip.
 */
@Composable
fun DepthGauge(modifier: Modifier) {
  val measurer = rememberTextMeasurer()
  val shallow = PoolPalette.Water.last()
  val deepest = deepestMarkColor(MaterialTheme.colorScheme)
  Canvas(modifier) {
    val lineX = size.width - 10.dp.toPx()
    drawLine(
        Brush.verticalGradient(listOf(shallow, deepest)),
        Offset(lineX, 0f),
        Offset(lineX, size.height),
        strokeWidth = 1.5.dp.toPx(),
    )
    PoolDepths.forEachIndexed { index, depth ->
      val fraction = index.toFloat() / PoolDepths.lastIndex
      val color = lerp(shallow, deepest, fraction)
      val y = size.height * fraction
      drawLine(color, Offset(lineX, y), Offset(lineX + 4.dp.toPx(), y), 1.5.dp.toPx())
      val text =
          measurer.measure(
              depthLabel(depth),
              style = TextStyle(color = color, fontSize = 9.sp, letterSpacing = 0.5.sp),
          )
      // Written downwards along the line, just below its tick (just above it for the last one).
      val labelY = if (index == PoolDepths.lastIndex) y - text.size.width - 4f else y + 4f
      val anchor = Offset(lineX + 2.dp.toPx(), labelY)
      rotate(90f, pivot = anchor) { drawText(text, topLeft = anchor) }
    }
  }
}

/**
 * The lane lines painted on a pool floor, seen from above through the water: two stripes across the
 * element in [color], each ending in the "T" that warns swimmers the wall is coming.
 */
fun Modifier.laneLines(color: Color): Modifier = drawBehind {
  val margin = 28.dp.toPx()
  for (lane in 1..2) {
    val y = size.height * lane / 3
    drawLaneLine(
        from = Offset(margin, y),
        to = Offset(size.width - margin, y),
        width = 5.dp.toPx(),
        barLength = 22.dp.toPx(),
        color = color,
        barAtStart = true,
    )
  }
}

/**
 * One lane line, horizontal or vertical, from [from] to [to]: a stripe [width] wide ending in a T
 * bar [barLength] long at [to], and at [from] too when [barAtStart].
 */
private fun DrawScope.drawLaneLine(
    from: Offset,
    to: Offset,
    width: Float,
    barLength: Float,
    color: Color,
    barAtStart: Boolean,
) {
  val horizontal = from.y == to.y
  val corner = CornerRadius(width / 2)
  val stripe = if (horizontal) Size(to.x - from.x, width) else Size(width, to.y - from.y)
  val halfWidth = if (horizontal) Offset(0f, width / 2) else Offset(width / 2, 0f)
  drawRoundRect(color, from - halfWidth, stripe, corner)
  val bar = if (horizontal) Size(width, barLength) else Size(barLength, width)
  val ends = if (barAtStart) listOf(from, to) else listOf(to)
  ends.forEach { end ->
    drawRoundRect(color, end - Offset(bar.width / 2, bar.height / 2), bar, corner)
  }
}

/** The near-black of the lane line, like the dark tiles on a pool floor. */
private val LANE_MARKER_COLOR = Color(0xFF04293A)

/** The black line on the floor of a lane, ending in a T at the wall, seen from above. */
@Composable
fun LaneMarker(modifier: Modifier) {
  Canvas(modifier) {
    val width = 8.dp.toPx()
    drawLaneLine(
        from = Offset(size.width / 2, 0f),
        to = Offset(size.width / 2, size.height - width / 2),
        width = width,
        barLength = 44.dp.toPx(),
        color = LANE_MARKER_COLOR,
        barAtStart = false,
    )
  }
}
