package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.ui.theme.loopingFraction
import com.github.se.pooltrack.ui.theme.poolTileColors
import com.github.se.pooltrack.ui.theme.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.sin

/** The week streaks at which the flame grows: each one reached is a tier of [streakTier]. */
val STREAK_TIERS = listOf(1, 5, 10, 20, 50, 75, 100)

/** How many of [STREAK_TIERS] a streak of [weeks] has reached: 0 without a streak, up to 7. */
fun streakTier(weeks: Int): Int = STREAK_TIERS.count { weeks >= it }

/**
 * What the banner says under the streak: how to start one, how many weeks are left before the flame
 * grows again, or that the top flame is reached.
 */
fun streakHint(weeks: Int): String {
  if (weeks <= 0) return "Swim this week to light it"
  val next = STREAK_TIERS.firstOrNull { it > weeks } ?: return "Top flame reached"
  return "${next - weeks} more to the next flame"
}

/**
 * How the flame and its banner look at a tier: the flame's [color] and [size], the warm [tint]
 * washing the banner from the flame's side, and the effects that join in as the streak grows.
 */
data class StreakLook(
    val color: Color,
    val size: Dp,
    val tint: Color = Color.Transparent,
    val flickerMillis: Int? = null,
    val shine: Boolean = false,
    val halo: Color? = null,
    val border: Color? = null,
    val sparkles: Color? = null,
)

/** The [StreakLook] of each tier of [streakTier], from no streak to the top flame. */
val STREAK_LOOKS =
    listOf(
        StreakLook(color = Color(0xFF6B8796), size = 28.dp),
        StreakLook(color = Color(0xFFF2B84B), size = 30.dp, tint = Color(0x14F2B84B)),
        StreakLook(color = Color(0xFFF59E0B), size = 32.dp, tint = Color(0x26F59E0B)),
        StreakLook(
            color = Color(0xFFF97316),
            size = 34.dp,
            tint = Color(0x33F97316),
            flickerMillis = 1_400,
        ),
        StreakLook(
            color = Color(0xFFEF4444),
            size = 36.dp,
            tint = Color(0x40EF4444),
            flickerMillis = 1_400,
            shine = true,
        ),
        StreakLook(
            color = Color(0xFFE11D48),
            size = 38.dp,
            tint = Color(0x4DE11D48),
            flickerMillis = 1_400,
            shine = true,
            halo = Color(0x66E11D48),
            border = Color(0xFFF2B84B),
        ),
        StreakLook(
            color = Color(0xFF8B5CF6),
            size = 40.dp,
            tint = Color(0x598B5CF6),
            flickerMillis = 900,
            shine = true,
            halo = Color(0x808B5CF6),
            border = Color(0xFFC4B5FD),
            sparkles = Color.White,
        ),
        StreakLook(
            color = Color(0xFFFDE047),
            size = 42.dp,
            tint = Color(0x66F2B84B),
            flickerMillis = 900,
            shine = true,
            halo = Color(0x99FDE047),
            border = Color(0xFFFDE047),
            sparkles = Color(0xFFFDE047),
        ),
    )

/**
 * The week streak as a low banner across Home: a flame that grows with [weeks] (see
 * [STREAK_LOOKS]), the number of weeks, and [streakHint]. The flame flickers from 10 weeks on, a
 * light sweeps the banner from 20, a halo pulses from 50 and sparkles join from 75.
 */
@Composable
internal fun StreakBanner(weeks: Int, modifier: Modifier = Modifier) {
  val look = STREAK_LOOKS[streakTier(weeks)]
  val flame = remember(look.color) { flameIcon(look.color) }
  val animate = rememberAnimationsEnabled()
  val flicker =
      loopingFraction(look.flickerMillis ?: 1, animate && look.flickerMillis != null, "flicker")
  val effects = loopingFraction(3_500, animate && (look.shine || look.halo != null), "streakFx")
  Card(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
      colors = poolTileColors(),
      border =
          BorderStroke(
              1.dp,
              look.border ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          ),
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth().drawBehind {
              drawRect(Brush.horizontalGradient(listOf(look.tint, Color.Transparent)))
              look.halo?.let { halo ->
                val pulse = 0.5f + 0.5f * sin(effects.value * 2 * PI.toFloat())
                drawRect(
                    Brush.radialGradient(
                        listOf(halo.copy(alpha = halo.alpha * pulse), Color.Transparent),
                        center = Offset(size.height / 2, size.height / 2),
                        radius = size.width / 2,
                    )
                )
              }
            },
    ) {
      // The flame again, large and faint in the corner, like the motif of the other tiles.
      Box(modifier = Modifier.matchParentSize()) {
        Image(
            imageVector = flame,
            contentDescription = null,
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 22.dp)
                    .size(96.dp)
                    .rotate(-15f)
                    .alpha(0.16f),
        )
      }
      if (look.shine) ShineSweep(effects.value, Modifier.matchParentSize())
      look.sparkles?.let { Sparkles(it, effects.value, Modifier.matchParentSize()) }
      Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Image(
            imageVector = flame,
            contentDescription = null,
            modifier =
                Modifier.size(look.size).graphicsLayer {
                  val wave = sin(flicker.value * 2 * PI.toFloat())
                  scaleX = 1f + 0.08f * wave
                  scaleY = 1f + 0.08f * wave
                  rotationZ = 3f * wave
                  transformOrigin = TransformOrigin(0.5f, 0.9f)
                },
        )
        Text(
            text = "$weeks",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 14.dp),
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
          Text(text = "Week streak", style = MaterialTheme.typography.bodyMedium)
          Text(
              text = streakHint(weeks),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

/** A band of light sweeping across the banner once per loop of [progress], then resting. */
@Composable
private fun ShineSweep(progress: Float, modifier: Modifier) {
  Canvas(modifier) {
    // The band crosses during the first half of the loop and is gone for the second.
    val travel = (progress / 0.55f).coerceAtMost(1f)
    val band = size.width * 0.35f
    val left = -band + (size.width + band) * travel
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
            startX = left,
            endX = left + band,
        )
    )
  }
}

/**
 * A few sparks in [color] twinkling across the banner, each one at its own moment of [progress].
 */
@Composable
private fun Sparkles(color: Color, progress: Float, modifier: Modifier) {
  Canvas(modifier) {
    SPARKS.forEach { (x, y, delay) ->
      val twinkle = sin(((progress + delay) % 1f) * PI.toFloat())
      drawCircle(
          color = color.copy(alpha = twinkle),
          radius = 2.dp.toPx() * twinkle,
          center = Offset(size.width * x, size.height * y),
      )
    }
  }
}

/**
 * Where each spark sits on the banner (as fractions of its size) and when in the loop it shines.
 */
private val SPARKS =
    listOf(
        Triple(0.18f, 0.2f, 0f),
        Triple(0.46f, 0.7f, 0.2f),
        Triple(0.7f, 0.18f, 0.4f),
        Triple(0.88f, 0.64f, 0.6f),
        Triple(0.6f, 0.4f, 0.8f),
    )
