package com.github.se.pooltrack.model.subscription

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionOrderingTest {

  private fun sub(id: String, addedAt: Long, expiresAt: Long?) =
      Subscription(
          id = id,
          uri = "content://$id",
          displayName = id,
          addedAtEpochMilli = addedAt,
          expiresAtEpochMilli = expiresAt,
      )

  private fun List<Subscription>.ids() = map { it.id }

  @Test
  fun farthestExpirationComesFirst() {
    val list = listOf(sub("soon", 3, 100), sub("far", 1, 300), sub("mid", 2, 200))

    assertEquals(listOf("far", "mid", "soon"), list.sortedByExpirationDescending().ids())
  }

  @Test
  fun subscriptionsWithoutExpiryComeFirst() {
    val list = listOf(sub("dated", 1, 300), sub("open", 2, null))

    assertEquals(listOf("open", "dated"), list.sortedByExpirationDescending().ids())
  }

  @Test
  fun expiredSubscriptionsComeLast() {
    val list = listOf(sub("expired", 5, 10), sub("future", 1, Long.MAX_VALUE - 1))

    assertEquals(listOf("future", "expired"), list.sortedByExpirationDescending().ids())
  }

  @Test
  fun tiesAreBrokenByMostRecentlyAdded() {
    val list =
        listOf(
            sub("a", 1, 100),
            sub("b", 2, 100),
            sub("c", 3, null),
            sub("d", 4, null),
        )

    assertEquals(listOf("d", "c", "b", "a"), list.sortedByExpirationDescending().ids())
  }

  @Test
  fun emptyListStaysEmpty() {
    assertEquals(
        emptyList<Subscription>(),
        emptyList<Subscription>().sortedByExpirationDescending(),
    )
  }
}
