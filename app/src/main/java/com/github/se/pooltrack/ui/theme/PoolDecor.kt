package com.github.se.pooltrack.ui.theme

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The pool look shared by every screen: a tiled floor, deep-water gradients, a lane rope of
// floats, depth marks along the wall, and water that moves. Everything is drawn with Compose
// itself (no images, fonts or libraries), and every animation stops when the system's animations
// are turned off.

/** The fixed colors of the pool, besides the theme's own [ColorScheme]. */
object PoolPalette {
  /** Deep water in the light theme, from the shallow end to the deep end. */
  val DeepLight = listOf(Color(0xFF2BA3CC), Color(0xFF0E6C8F), Color(0xFF073E57))

  /** Deep water in the dark theme: one step lighter than the page so it still stands out. */
  val DeepDark = listOf(Color(0xFF1B7FA6), Color(0xFF0E5675), Color(0xFF062E43))

  /** Text and icons on deep water. */
  val OnDeep = Color.White

  /** Secondary text on deep water. */
  val OnDeepVariant = Color(0xFFCBEFFC)

  /** The rope the lane floats are threaded on. */
  val Rope = Color(0xFF6E95A5)

  /** A float's lit side and its shaded side. */
  val FloatRed = Color(0xFFF26A5E) to Color(0xFFC9362C)
  val FloatBlue = Color(0xFF3DB2DA) to Color(0xFF0E6C8F)
  val FloatWhite = Color(0xFFFFFFFF) to Color(0xFFCFE3EB)

  /** The light spots that water casts on the floor. */
  val Caustic = Color(0x40B8ECFF)
}

/** Whether [scheme] is the dark theme, judged from its page color. */
fun isDarkScheme(scheme: ColorScheme): Boolean = scheme.background.luminance() < 0.5f

/** The deep-water colors that go with [scheme]. */
fun deepWaterColors(scheme: ColorScheme): List<Color> =
    if (isDarkScheme(scheme)) PoolPalette.DeepDark else PoolPalette.DeepLight

/** A diagonal deep-water gradient, for heroes, main buttons and the in-pool screen. */
fun deepWaterBrush(scheme: ColorScheme): Brush = Brush.linearGradient(deepWaterColors(scheme))

/** Whether the system lets apps animate (the developer option "Animator duration scale"). */
fun animationsEnabled(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/** [animationsEnabled] for the current context, read once. */
@Composable
fun rememberAnimationsEnabled(): Boolean {
  val resolver = LocalContext.current.contentResolver
  return remember(resolver) { animationsEnabled(resolver) }
}

/**
 * A value going from 0 to 1 every [durationMillis], forever, or holding still at 0.5 when [enabled]
 * is false. Read it while drawing, so that only the drawing is redone each frame.
 */
@Composable
fun loopingFraction(durationMillis: Int, enabled: Boolean, label: String): State<Float> {
  if (!enabled) return remember { mutableFloatStateOf(STILL_FRACTION) }
  return rememberInfiniteTransition(label)
      .animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
          label = label,
      )
}

private const val STILL_FRACTION = 0.5f
private const val TWO_PI = (2 * PI).toFloat()

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

private val TILE_SIZE = 24.dp
private const val TILE_LINE_ALPHA = 0.07f

/** The light that ripples on the floor of a pool: two soft spots drifting slowly. */
@Composable
fun Caustics(modifier: Modifier) {
  val phase = loopingFraction(9_000, rememberAnimationsEnabled(), "caustics")
  Canvas(modifier) {
    val angle = phase.value * TWO_PI
    val radius = size.maxDimension * 0.55f
    val spots =
        listOf(
            Offset(size.width * (0.2f + 0.08f * sin(angle)), size.height * (0.1f * cos(angle))),
            Offset(
                size.width * (0.85f - 0.08f * cos(angle)),
                size.height * (0.9f + 0.1f * sin(angle)),
            ),
        )
    spots.forEach { center ->
      drawCircle(
          Brush.radialGradient(
              listOf(PoolPalette.Caustic, Color.Transparent),
              center = center,
              radius = radius,
          ),
          radius = radius,
          center = center,
      )
    }
  }
}

/**
 * A surface of deep water, with [Caustics] moving on it when [caustics] is true. Its content is
 * drawn in [PoolPalette.OnDeep] by default.
 */
@Composable
fun DeepWater(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    caustics: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier.clip(shape).background(deepWaterBrush(MaterialTheme.colorScheme), shape)) {
    if (caustics) Caustics(Modifier.matchParentSize())
    CompositionLocalProvider(LocalContentColor provides PoolPalette.OnDeep) { content() }
  }
}

/** The kind of a float on a lane rope. */
enum class LaneFloat {
  /** The red floats at both ends of a lane, warning that the wall is close. */
  End,
  Blue,
  White,
}

/** The float at [index] of a rope of [count] floats: red ends, then blue and white alternating. */
fun laneFloatAt(index: Int, count: Int, endFloats: Int = LANE_END_FLOATS): LaneFloat =
    when {
      index < endFloats || index >= count - endFloats -> LaneFloat.End
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
