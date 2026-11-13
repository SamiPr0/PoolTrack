package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** The kind of a float on a lane rope. */
enum class LaneFloat {
  /** The red floats at both ends of a lane, warning that the wall is close. */
  End,
  Blue,
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

private const val LANE_END_FLOATS = 3
private const val LANE_FLOATS = 22

/**
 * A lane rope across the screen: a row of floats on a thin rope. When [animated], each float bobs a
 * little after its neighbour, so a gentle wave runs along the rope.
 */
@Composable
fun LaneRope(modifier: Modifier, animated: Boolean) {
  val phase = loopingFraction(2_800, animated && rememberAnimationsEnabled(), "laneRope")
  Canvas(modifier.fillMaxWidth().height(14.dp)) {
    val middle = size.height / 2
    drawLine(PoolPalette.Rope, Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
    val floatWidth = 9.dp.toPx()
    val floatHeight = 10.dp.toPx()
    val step = (size.width - floatWidth) / (LANE_FLOATS - 1)
    val amplitude = 1.5.dp.toPx()
    repeat(LANE_FLOATS) { index ->
      val (lit, shade) = laneFloatColors(laneFloatAt(index, LANE_FLOATS))
      val bob = -amplitude * sin((phase.value + index * 0.04f) * TWO_PI)
      val topLeft = Offset(index * step, middle - floatHeight / 2 + bob)
      drawRoundRect(
          Brush.radialGradient(
              listOf(lit, shade),
              center = topLeft + Offset(floatWidth * 0.35f, floatHeight * 0.3f),
              radius = floatWidth,
          ),
          topLeft = topLeft,
          size = Size(floatWidth, floatHeight),
          cornerRadius = CornerRadius(floatWidth / 2),
      )
    }
  }
}
