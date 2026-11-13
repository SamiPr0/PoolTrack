package com.github.se.pooltrack.ui.home

import com.github.se.pooltrack.model.subscription.Subscription
import java.time.Instant
import kotlin.math.roundToInt

/**
 * How much of [subscription] is left, from 1 (all of it) to 0 (none), shown as the water level of
 * Home's pass card. A dated pass empties with time, from when it was added to when it expires; an
 * entry-limited one with each of its [usedEntries]. The date wins when both are set, like in the
 * card's message. `null` when the pass has neither, so there is nothing to measure.
 */
fun passWaterLevel(
    subscription: Subscription,
    usedEntries: Int,
    now: Instant = Instant.now(),
): Float? {
  val expires = subscription.expiresAtEpochMilli
  if (expires != null) {
    val total = expires - subscription.addedAtEpochMilli
    if (total <= 0) return if (now.toEpochMilli() < expires) 1f else 0f
    return ((expires - now.toEpochMilli()).toFloat() / total).coerceIn(0f, 1f)
  }
  val max = subscription.maxEntries ?: return null
  if (max <= 0) return 0f
  return ((max - usedEntries).toFloat() / max).coerceIn(0f, 1f)
}

/** "68%": a [passWaterLevel] as a whole percentage. */
fun passWaterLabel(level: Float): String = "${(level * 100).roundToInt()}%"
