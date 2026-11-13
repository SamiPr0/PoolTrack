package com.github.se.pooltrack.ui.theme

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
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

  /** An expired date written on deep water, where the theme's error red is too dark to read. */
  val ExpiredOnDeep = Color(0xFFFFB4AB)

  /** The water filling a card, from its surface to its bottom. */
  val Water = listOf(Color(0xFFBDE7F7), Color(0xFF8FD2EC))

  /** The water of a pass that is about to run out. */
  val WaterUrgent = listOf(Color(0xFFFFD9D3), Color(0xFFF7B4AA))

  /** The rope the lane floats are threaded on. */
  val Rope = Color(0xFF6E95A5)

  /** A float's lit side and its shaded side. */
  val FloatRed = Color(0xFFF26A5E) to Color(0xFFC9362C)
  val FloatBlue = Color(0xFF3DB2DA) to Color(0xFF0E6C8F)
  val FloatWhite = Color(0xFFFFFFFF) to Color(0xFFCFE3EB)

  /** Accents for the stat tiles, like the colored tiles of a pool's mosaic. */
  val AccentTeal = Color(0xFF2BB3A3)
  val AccentSun = Color(0xFFF2B84B)
  val AccentShallow = Color(0xFF2BA3CC)
  val AccentDeep = Color(0xFF0E6C8F)

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

/**
 * A lane rope across the screen: a row of floats on a thin rope. When [animated], each float bobs a
 * little after its neighbour, so a gentle wave runs along the rope.
 */
@Composable
fun LaneRope(modifier: Modifier, animated: Boolean) {
  val floatCount = LANE_FLOATS
  val phase = loopingFraction(2_800, animated && rememberAnimationsEnabled(), "laneRope")
  Canvas(modifier.fillMaxWidth().height(14.dp)) {
    val middle = size.height / 2
    drawLine(PoolPalette.Rope, Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
    val floatWidth = 9.dp.toPx()
    val floatHeight = 10.dp.toPx()
    val step = (size.width - floatWidth) / (floatCount - 1)
    val amplitude = 1.5.dp.toPx()
    repeat(floatCount) { index ->
      val (lit, shade) = laneFloatColors(laneFloatAt(index, floatCount))
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

/** The depths marked along the wall, from the shallow end to the deep end, in metres. */
val PoolDepths = listOf(0.8, 1.2, 1.5, 1.8, 2.2)

/** "1.5 m": how a depth is written on the wall. */
fun depthLabel(meters: Double): String = String.format(Locale.ROOT, "%.1f m", meters)

/**
 * The depth marks painted along a pool's wall, drawn down the right edge of a screen: a line that
 * darkens with depth, a tick for each of [PoolDepths] and its label written vertically next to it.
 * It is meant to sit behind the content, in a narrow strip.
 */
@Composable
fun DepthGauge(modifier: Modifier) {
  val depths = PoolDepths
  val measurer = rememberTextMeasurer()
  val deep = deepWaterColors(MaterialTheme.colorScheme)
  val shallow = PoolPalette.Water.last()
  Canvas(modifier) {
    val lineX = size.width - 10.dp.toPx()
    drawLine(
        Brush.verticalGradient(listOf(shallow, deep[1], deep[2])),
        Offset(lineX, 0f),
        Offset(lineX, size.height),
        strokeWidth = 1.5.dp.toPx(),
    )
    val last = (depths.size - 1).coerceAtLeast(1)
    depths.forEachIndexed { index, depth ->
      val fraction = index.toFloat() / last
      val color = lerp(shallow, deep[2], fraction)
      val y = size.height * fraction
      drawLine(color, Offset(lineX, y), Offset(lineX + 4.dp.toPx(), y), 1.5.dp.toPx())
      val text =
          measurer.measure(
              depthLabel(depth),
              style = TextStyle(color = color, fontSize = 9.sp, letterSpacing = 0.5.sp),
          )
      // Written downwards along the line, just below its tick (just above it for the last one).
      val labelY = if (index == depths.lastIndex) y - text.size.width - 4f else y + 4f
      val anchor = Offset(lineX + 2.dp.toPx(), labelY)
      rotate(90f, pivot = anchor) { drawText(text, topLeft = anchor) }
    }
  }
}

/**
 * Water filling its container up to [level] (0 to 1), with a slow wave on its surface. The level
 * itself animates when it changes.
 */
@Composable
fun WaterFill(level: Float, colors: List<Color>, modifier: Modifier) {
  val phase = loopingFraction(7_000, rememberAnimationsEnabled(), "waterFill")
  val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), tween(900), label = "waterLevel")
  Canvas(modifier) {
    val surface = size.height * (1f - animatedLevel)
    val amplitude = 3.dp.toPx()
    val wavelength = 50.dp.toPx()
    val path =
        Path().apply {
          moveTo(0f, size.height)
          var x = 0f
          while (x <= size.width) {
            lineTo(x, surface + amplitude * sin((x / wavelength + phase.value) * TWO_PI))
            x += WAVE_STEP_PX
          }
          lineTo(size.width, surface + amplitude * sin((size.width / wavelength) * TWO_PI))
          lineTo(size.width, size.height)
          close()
        }
    drawPath(path, Brush.verticalGradient(colors, startY = surface, endY = size.height))
  }
}

private const val WAVE_STEP_PX = 4f

/** Rings spreading from a button like ripples on water. Put it behind the button, same size. */
@Composable
fun RippleRings(modifier: Modifier) {
  val phase = loopingFraction(2_400, rememberAnimationsEnabled(), "rippleRings")
  Canvas(modifier) {
    listOf(0f, 0.5f).forEach { offset ->
      val progress = (phase.value + offset) % 1f
      val scale = 1f + 0.35f * progress
      val ringSize = Size(size.width * scale, size.height * scale)
      drawRoundRect(
          color = PoolPalette.AccentShallow.copy(alpha = 0.7f * (1f - progress)),
          topLeft = Offset((size.width - ringSize.width) / 2, (size.height - ringSize.height) / 2),
          size = ringSize,
          cornerRadius = CornerRadius(ringSize.height / 2),
          style = Stroke(width = 2.dp.toPx()),
      )
    }
  }
}

/** A few bubbles rising slowly through the water. */
@Composable
fun Bubbles(modifier: Modifier) {
  val phase = loopingFraction(9_000, rememberAnimationsEnabled(), "bubbles")
  Canvas(modifier) {
    BUBBLES.forEach { (x, start, radius) ->
      val progress = (phase.value + start) % 1f
      val alpha = if (progress < 0.15f) progress / 0.15f else 1f - progress
      drawCircle(
          color = PoolPalette.OnDeepVariant.copy(alpha = 0.6f * alpha),
          radius = radius.dp.toPx(),
          center = Offset(size.width * x, size.height * (1f - progress)),
          style = Stroke(width = 1.dp.toPx()),
      )
    }
  }
}

/** Each bubble's horizontal position, where in the loop it starts, and its radius in dp. */
private val BUBBLES =
    listOf(
        Triple(0.15f, 0f, 3f),
        Triple(0.4f, 0.35f, 2f),
        Triple(0.62f, 0.7f, 4f),
        Triple(0.85f, 0.2f, 2.5f),
    )

private val LANE_MARKER_COLOR = Color(0xFF04293A)

/** The black line on the floor of a lane, ending in a T at the wall, seen from above. */
@Composable
fun LaneMarker(modifier: Modifier) {
  val color = LANE_MARKER_COLOR
  Canvas(modifier) {
    val width = 8.dp.toPx()
    val x = size.width / 2 - width / 2
    drawRoundRect(color, Offset(x, 0f), Size(width, size.height), CornerRadius(width / 2))
    val barWidth = 44.dp.toPx()
    drawRoundRect(
        color,
        Offset(size.width / 2 - barWidth / 2, size.height - width),
        Size(barWidth, width),
        CornerRadius(width / 2),
    )
  }
}
