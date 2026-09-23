package com.github.se.pooltrack.model.subscription

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionTest {

  private val now = Instant.parse("2026-09-23T23:00:00Z")

  private fun subscription(
      expiresAt: Instant? = null,
      maxEntries: Int? = null,
      price: Double? = null,
  ) =
      Subscription(
          id = "id",
          uri = "content://pass",
          displayName = "Pass",
          addedAtEpochMilli = 1_000L,
          expiresAtEpochMilli = expiresAt?.toEpochMilli(),
          maxEntries = maxEntries,
          price = price,
      )

  @Test
  fun addedAt_isTheAddedInstant() {
    assertEquals(Instant.ofEpochMilli(1_000L), subscription().addedAt)
  }

  @Test
  fun expiresAt_isNull_whenNoExpirationDate() {
    assertNull(subscription().expiresAt)
  }

  @Test
  fun isExpired_isTrue_whenExpirationDateHasPassed() {
    assertTrue(subscription(expiresAt = Instant.now().minusSeconds(60)).isExpired)
  }

  @Test
  fun isExpired_isFalse_whenExpirationDateIsAhead() {
    assertFalse(subscription(expiresAt = Instant.now().plusSeconds(3_600)).isExpired)
  }

  @Test
  fun isExpired_isFalse_whenNoExpirationDate() {
    assertFalse(subscription().isExpired)
  }

  @Test
  fun pricePerEntry_dividesPriceByEntries_whenBothAreSet() {
    assertEquals(6.0, subscription(maxEntries = 10, price = 60.0).pricePerEntry!!, 0.0)
  }

  @Test
  fun pricePerEntry_isNull_whenPriceOrEntriesMissing() {
    assertNull(subscription(maxEntries = 10).pricePerEntry)
    assertNull(subscription(price = 60.0).pricePerEntry)
  }

  @Test
  fun pricePerEntry_isNull_whenEntriesIsNotPositive() {
    assertNull(subscription(maxEntries = 0, price = 60.0).pricePerEntry)
    assertNull(subscription(maxEntries = -1, price = 60.0).pricePerEntry)
  }

  @Test
  fun daysUntilExpiration_countsCalendarDays_whenExpirationIsAhead() {
    // 23:00 on the 23rd to midnight on the 30th: 7 calendar days, though under 7 * 24 hours.
    val expiresAt = Instant.parse("2026-09-30T00:00:00Z")

    assertEquals(7L, subscription(expiresAt = expiresAt).daysUntilExpiration(now, ZoneOffset.UTC))
  }

  @Test
  fun daysUntilExpiration_isNegative_whenExpired() {
    val expiresAt = Instant.parse("2026-09-21T12:00:00Z")

    assertEquals(-2L, subscription(expiresAt = expiresAt).daysUntilExpiration(now, ZoneOffset.UTC))
  }

  @Test
  fun daysUntilExpiration_isNull_whenNoExpirationDate() {
    assertNull(subscription().daysUntilExpiration(now, ZoneOffset.UTC))
  }
}
