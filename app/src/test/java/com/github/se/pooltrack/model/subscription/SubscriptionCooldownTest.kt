package com.github.se.pooltrack.model.subscription

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionCooldownTest {

  private val now = Instant.parse("2026-09-23T12:00:00Z")

  @Test
  fun remainingSubscriptionCooldown_isNull_whenNeverEntered() {
    assertNull(remainingSubscriptionCooldown(lastEnteredAt = null, now = now))
  }

  @Test
  fun remainingSubscriptionCooldown_isTheFullCooldown_whenJustEntered() {
    assertEquals(SUBSCRIPTION_OPEN_COOLDOWN, remainingSubscriptionCooldown(now, now))
  }

  @Test
  fun remainingSubscriptionCooldown_isWhatRemains_whenEnteredWithinTheCooldown() {
    val lastEnteredAt = now.minus(Duration.ofHours(4).plusMinutes(59))

    assertEquals(Duration.ofMinutes(1), remainingSubscriptionCooldown(lastEnteredAt, now))
  }

  @Test
  fun remainingSubscriptionCooldown_isNull_whenCooldownEndsExactlyNow() {
    assertNull(remainingSubscriptionCooldown(now.minus(SUBSCRIPTION_OPEN_COOLDOWN), now))
  }

  @Test
  fun remainingSubscriptionCooldown_isNull_whenCooldownIsOver() {
    assertNull(remainingSubscriptionCooldown(now.minus(Duration.ofHours(6)), now))
  }

  @Test
  fun remainingSubscriptionCooldown_isNull_whenNeverEnteredAndNowIsTheSystemClock() {
    assertNull(remainingSubscriptionCooldown(lastEnteredAt = null))
  }

  @Test
  fun remainingSubscriptionCooldown_isNull_whenLastEntryWasADayAgoAndNowIsTheSystemClock() {
    assertNull(remainingSubscriptionCooldown(Instant.now().minus(Duration.ofDays(1))))
  }

  @Test
  fun remainingSubscriptionCooldown_isAtMostTheCooldown_whenJustEnteredAndNowIsTheSystemClock() {
    val remaining = remainingSubscriptionCooldown(Instant.now())

    assertTrue(remaining != null && remaining <= SUBSCRIPTION_OPEN_COOLDOWN)
    assertTrue(remaining!! > Duration.ofHours(4))
  }
}
