package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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
