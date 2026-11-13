package com.github.se.pooltrack.ui.theme

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI

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
