package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance

// The pool look shared by every screen, drawn with Compose itself (no images, fonts or libraries):
// its colors here, and its floor, water, lane rope and motion in the other Pool*.kt files.

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

  /** A red end float of a lane rope: its lit side, then its shaded side. */
  val FloatRed = Color(0xFFF26A5E) to Color(0xFFC9362C)

  /** A blue float of a lane rope: its lit side, then its shaded side. */
  val FloatBlue = Color(0xFF3DB2DA) to Color(0xFF0E6C8F)

  /** A white float of a lane rope: its lit side, then its shaded side. */
  val FloatWhite = Color(0xFFFFFFFF) to Color(0xFFCFE3EB)

  // Accents for the stat tiles, like the colored tiles of a pool's mosaic.

  /** A teal mosaic tile. */
  val AccentTeal = Color(0xFF2BB3A3)

  /** A sunny yellow mosaic tile, like the light on the water. */
  val AccentSun = Color(0xFFF2B84B)

  /** A tile of shallow water: the lightest of [DeepLight]. */
  val AccentShallow = DeepLight.first()

  /** A tile of deep water: the middle of [DeepLight]. */
  val AccentDeep = DeepLight[1]

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

/**
 * The color of a tile laid on [poolWaves]: white in the light theme, and in the dark theme a step
 * lighter than the page, so cards stand out from the water instead of sinking into it.
 */
fun poolTileColor(scheme: ColorScheme): Color =
    if (isDarkScheme(scheme)) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest

/** The colors of a card that sits on [poolWaves], a tile of [poolTileColor]. */
@Composable
fun poolTileColors(): CardColors =
    CardDefaults.cardColors(
        containerColor = poolTileColor(MaterialTheme.colorScheme),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Fills the element with [deepWaterBrush] in [shape], e.g. a button made transparent for it. */
fun Modifier.deepWater(scheme: ColorScheme, shape: Shape): Modifier =
    background(deepWaterBrush(scheme), shape)
