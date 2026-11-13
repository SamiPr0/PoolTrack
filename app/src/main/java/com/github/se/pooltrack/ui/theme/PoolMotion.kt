package com.github.se.pooltrack.ui.theme

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin

// How the pool moves. Every animation goes through loopingFraction, which holds still when the
// system's animations are turned off.

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

/** Moves the element up and down by [amplitude] as [phase] loops, like something afloat. */
fun Modifier.bobbing(phase: State<Float>, amplitude: Dp): Modifier = graphicsLayer {
  translationY = -amplitude.toPx() * sin(phase.value * TWO_PI)
}

private const val STILL_FRACTION = 0.5f

/** A full turn, in radians: one loop of [loopingFraction] as an angle. */
internal const val TWO_PI = (2 * PI).toFloat()
