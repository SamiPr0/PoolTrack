package com.github.se.pooltrack.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// The water of the pool: deep-water surfaces, moving light, waves, ripples and bubbles.

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
  Box(modifier.clip(shape).deepWater(MaterialTheme.colorScheme, shape)) {
    if (caustics) Caustics(Modifier.matchParentSize())
    CompositionLocalProvider(LocalContentColor provides PoolPalette.OnDeep) { content() }
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

/** The horizontal step, in pixels, between two points of a wave's outline. */
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
