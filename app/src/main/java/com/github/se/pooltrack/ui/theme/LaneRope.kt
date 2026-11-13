package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** The kind of a float on a lane rope. */
enum class LaneFloat {
  /** The red floats at both ends of a lane, warning that the wall is close. */
  End,

  /** The blue floats of the middle of the lane, every other one. */
  Blue,

  /** The white floats between the blue ones. */
  White,
}

/** The float at [index] of a rope of [count] floats: red ends, then blue and white alternating. */
fun laneFloatAt(index: Int, count: Int): LaneFloat =
    when {
      index < LANE_END_FLOATS || index >= count - LANE_END_FLOATS -> LaneFloat.End
      index % 2 == 0 -> LaneFloat.Blue
      else -> LaneFloat.White
    }

/** The lit and shaded colors of a [LaneFloat]. */
fun laneFloatColors(kind: LaneFloat): Pair<Color, Color> =
    when (kind) {
      LaneFloat.End -> PoolPalette.FloatRed
      LaneFloat.Blue -> PoolPalette.FloatBlue
      LaneFloat.White -> PoolPalette.FloatWhite
    }

/** How many red floats sit at each end of a rope. */
private const val LANE_END_FLOATS = 3
/** The length of a float along the rope: a capsule twice as long as it is tall. */
private val FLOAT_LENGTH = 16.dp

/** The height of a float. */
private val FLOAT_HEIGHT = 8.dp

/** The space left between two floats. */
private val FLOAT_GAP = 4.dp

/**
 * How many floats of [floatLength] fit along a rope [width] long, [gap] apart. At least one, so a
 * rope is never bare.
 */
fun laneFloatCount(width: Float, floatLength: Float, gap: Float): Int =
    ((width + gap) / (floatLength + gap)).toInt().coerceAtLeast(1)

/**
 * A lane rope across the screen: capsule-shaped floats threaded on a thin rope, centred so both
 * ends match. Each float bobs a little after its neighbour, so a gentle wave runs along the rope.
 * It holds still when the system's animations are turned off.
 */
@Composable
fun LaneRope(modifier: Modifier) {
  val phase = loopingFraction(2_800, rememberAnimationsEnabled(), "laneRope")
  Canvas(modifier.fillMaxWidth().height(14.dp)) {
    val middle = size.height / 2
    drawLine(PoolPalette.Rope, Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
    val length = FLOAT_LENGTH.toPx()
    val height = FLOAT_HEIGHT.toPx()
    val gap = FLOAT_GAP.toPx()
    val count = laneFloatCount(size.width, length, gap)
    val start = (size.width - (count * length + (count - 1) * gap)) / 2
    val amplitude = 1.5.dp.toPx()
    repeat(count) { index ->
      val (lit, shade) = laneFloatColors(laneFloatAt(index, count))
      val bob = -amplitude * sin((phase.value + index * 0.04f) * TWO_PI)
      val top = middle - height / 2 + bob
      // Lit from above: the top of the capsule is brighter than its underside.
      drawRoundRect(
          Brush.verticalGradient(
              listOf(lit, lerp(lit, shade, 0.6f)),
              startY = top,
              endY = top + height,
          ),
          topLeft = Offset(start + index * (length + gap), top),
          size = Size(length, height),
          cornerRadius = CornerRadius(height / 2),
      )
    }
  }
}
