package com.github.se.pooltrack.model.subscription

import java.time.Duration
import java.time.Instant

/** The subscription pass can't be reopened for this long after it was last shown. */
val SUBSCRIPTION_OPEN_COOLDOWN: Duration = Duration.ofHours(5)

/**
 * How much longer the subscription pass stays locked, or `null` if it can be opened right now.
 *
 * @param lastOpenedAt When the pass was last shown, or `null` if it never was.
 * @param now The instant to check against.
 */
fun remainingSubscriptionCooldown(lastOpenedAt: Instant?, now: Instant = Instant.now()): Duration? {
  if (lastOpenedAt == null) return null
  val elapsed = Duration.between(lastOpenedAt, now)
  val remaining = SUBSCRIPTION_OPEN_COOLDOWN.minus(elapsed)
  return if (remaining.isNegative || remaining.isZero) null else remaining
}
