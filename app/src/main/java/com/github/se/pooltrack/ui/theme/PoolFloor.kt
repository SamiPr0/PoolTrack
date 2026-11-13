package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// The water and walls of the pool: waves, depth tags and the lane line.

/**
 * Paints the pool behind a screen: [scheme]'s page color, faint waves rolling across it, and a
 * gradient that gets slightly deeper toward the bottom, like the water getting deeper.
 */
fun Modifier.poolWaves(scheme: ColorScheme): Modifier = drawBehind {
  drawRect(scheme.background)
  val spacing = WAVE_SPACING.toPx()
  val amplitude = 4.dp.toPx()
  val half = WAVE_LENGTH.toPx() / 2
  val stroke = Stroke(width = 2.dp.toPx())
  var y = spacing / 2
  var row = 0
  while (y < size.height) {
    // Every other wave is shifted by half a wavelength, so they don't look stacked.
    var x = if (row % 2 == 0) 0f else -half
    val wave = Path().apply { moveTo(x, y) }
    var up = true
    while (x < size.width) {
      wave.quadraticTo(x + half / 2, if (up) y - amplitude else y + amplitude, x + half, y)
      x += half
      up = !up
    }
    drawPath(wave, scheme.primary.copy(alpha = WAVE_ALPHA), style = stroke)
    y += spacing
    row++
  }
  drawRect(
      Brush.verticalGradient(
          0f to Color.Transparent,
          0.55f to scheme.primary.copy(alpha = 0.05f),
          1f to scheme.primary.copy(alpha = 0.12f),
      )
  )
}

/** The distance between two waves of [poolWaves]. */
private val WAVE_SPACING = 110.dp

/** The length of one wave of [poolWaves], crest to crest. */
private val WAVE_LENGTH = 124.dp

/** How strongly the waves of [poolWaves] show. */
private const val WAVE_ALPHA = 0.08f

/** The depths marked along the wall, from the shallow end to the deep end, in metres. */
val PoolDepths = listOf(0.8, 1.2, 1.5, 1.8, 2.2)

/** "1.5 m": how a depth is written on the wall. */
fun depthLabel(meters: Double): String = String.format(Locale.ROOT, "%.1f m", meters)

/**
 * The depths written on a pool's wall, down the right edge of a screen: a small tag for each of
 * [PoolDepths], spread from top to bottom and flush with the edge. It sits behind the content, in a
 * strip wide enough for a tag.
 */
@Composable
fun DepthTags(modifier: Modifier) {
  val measurer = rememberTextMeasurer()
  val tagColor = MaterialTheme.colorScheme.primaryContainer
  val style = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
  Canvas(modifier) {
    val padX = 6.dp.toPx()
    val padY = 2.dp.toPx()
    val corner = CornerRadius(4.dp.toPx())
    PoolDepths.forEachIndexed { index, depth ->
      val text = measurer.measure(depthLabel(depth), style)
      val tag = Size(text.size.width + padX * 2, text.size.height + padY * 2)
      val top = (size.height - tag.height) * index / PoolDepths.lastIndex
      val left = size.width - tag.width
      // Rounded on the content side only, so the tag looks stuck to the wall.
      drawPath(
          Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(left, top), tag),
                    topLeft = corner,
                    bottomLeft = corner,
                )
            )
          },
          tagColor,
      )
      drawText(text, topLeft = Offset(left + padX, top + padY))
    }
  }
}

/** The near-black of the lane line, like the dark tiles on a pool floor. */
private val LANE_MARKER_COLOR = Color(0xFF04293A)

/** The black line on the floor of a lane, ending in a T at the wall, seen from above. */
@Composable
fun LaneMarker(modifier: Modifier) {
  Canvas(modifier) {
    val width = 8.dp.toPx()
    val corner = CornerRadius(width / 2)
    drawRoundRect(
        LANE_MARKER_COLOR,
        Offset(size.width / 2 - width / 2, 0f),
        Size(width, size.height),
        corner,
    )
    val bar = 44.dp.toPx()
    drawRoundRect(
        LANE_MARKER_COLOR,
        Offset(size.width / 2 - bar / 2, size.height - width),
        Size(bar, width),
        corner,
    )
  }
}
