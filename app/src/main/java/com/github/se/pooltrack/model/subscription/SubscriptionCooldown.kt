package com.github.se.pooltrack.model.subscription

import java.time.Duration
import java.time.Instant

/** The subscription pass can't be reopened for this long after a confirmed pool entry. */
val SUBSCRIPTION_OPEN_COOLDOWN: Duration = Duration.ofHours(5)

/**
 * How much longer the subscription pass stays locked, or `null` if it can be opened right now.
 *
 * @param lastEnteredAt When the user last entered the pool - i.e. the most recent confirmed
 *   [com.github.se.pooltrack.model.entry.Entry]'s timestamp, not a separately tracked value, so
 *   deleting that entry immediately lifts the cooldown - or `null` if they never entered.
 * @param now The instant to check against.
 */
fun remainingSubscriptionCooldown(
    lastEnteredAt: Instant?,
    now: Instant = Instant.now(),
): Duration? {
  if (lastEnteredAt == null) return null
  val elapsed = Duration.between(lastEnteredAt, now)
  val remaining = SUBSCRIPTION_OPEN_COOLDOWN.minus(elapsed)
  return if (remaining.isNegative || remaining.isZero) null else remaining
}
