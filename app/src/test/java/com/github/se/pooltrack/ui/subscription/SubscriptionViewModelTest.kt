package com.github.se.pooltrack.ui.subscription

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubscriptionViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val older =
      Subscription(id = "old", uri = "content://old", displayName = "Old", addedAtEpochMilli = 1L)
  private val newer =
      Subscription(id = "new", uri = "content://new", displayName = "New", addedAtEpochMilli = 2L)

  @Test
  fun subscriptions_listsEverySubscriptionMostRecentFirst() {
    val viewModel =
        SubscriptionViewModel(
            FakeSubscriptionRepository(listOf(older, newer)),
            FakeEntryRepository(),
        )

    assertEquals(listOf(newer, older), viewModel.subscriptions.value)
  }

  @Test
  fun subscriptions_areSortedByExpirationFarthestFirst() {
    val soon = older.copy(id = "soon", expiresAtEpochMilli = 100L)
    val far = newer.copy(id = "far", expiresAtEpochMilli = 200L)
    val viewModel =
        SubscriptionViewModel(FakeSubscriptionRepository(listOf(far, soon)), FakeEntryRepository())

    assertEquals(listOf(far, soon), viewModel.subscriptions.value)
  }

  @Test
  fun activeSubscription_isTheActiveOne_whenOneIsActive() {
    val viewModel =
        SubscriptionViewModel(
            FakeSubscriptionRepository(listOf(older, newer), "old"),
            FakeEntryRepository(),
        )

    assertEquals(older, viewModel.activeSubscription.value)
  }

  @Test
  fun lastEntryTimestamp_isMostRecentEntry_whenEntriesRecorded() {
    val entries =
        FakeEntryRepository(
            listOf(Entry(timestampEpochMilli = 5_000L), Entry(timestampEpochMilli = 9_000L))
        )
    val viewModel = SubscriptionViewModel(FakeSubscriptionRepository(), entries)

    assertEquals(Instant.ofEpochMilli(9_000L), viewModel.lastEntryTimestamp.value)
  }

  @Test
  fun lastEntryTimestamp_isNull_whenNoEntriesRecorded() {
    val viewModel = SubscriptionViewModel(FakeSubscriptionRepository(), FakeEntryRepository())

    assertNull(viewModel.lastEntryTimestamp.value)
  }

  @Test
  fun entryCountsBySubscriptionId_countsTaggedEntries_ignoringUntaggedOnes() {
    val entries =
        FakeEntryRepository(
            listOf(
                Entry(timestampEpochMilli = 1L, subscriptionId = "old"),
                Entry(timestampEpochMilli = 2L, subscriptionId = "new"),
                Entry(timestampEpochMilli = 3L, subscriptionId = "new"),
                Entry(timestampEpochMilli = 4L, subscriptionId = null),
            )
        )
    val viewModel = SubscriptionViewModel(FakeSubscriptionRepository(), entries)

    assertEquals(mapOf("old" to 1, "new" to 2), viewModel.entryCountsBySubscriptionId.value)
  }

  @Test
  fun onSubscriptionPicked_addsSubscriptionWithEveryDetail_andMakesItActive() {
    val subscriptions = FakeSubscriptionRepository(listOf(older), "old")
    val viewModel = SubscriptionViewModel(subscriptions, FakeEntryRepository())

    viewModel.onSubscriptionPicked(
        uri = "content://picked",
        expiresAtEpochMilli = 42L,
        maxEntries = 10,
        price = 60.0,
    )

    val added = subscriptions.storedSubscriptions.last()
    assertEquals("content://picked", added.uri)
    assertEquals(42L, added.expiresAtEpochMilli)
    assertEquals(10, added.maxEntries)
    assertEquals(60.0, added.price!!, 0.0)
    assertEquals(added, viewModel.activeSubscription.value)
    assertEquals(2, viewModel.subscriptions.value.size)
  }

  @Test
  fun onSubscriptionPicked_leavesOptionalDetailsEmpty_whenNotGiven() {
    val subscriptions = FakeSubscriptionRepository()
    val viewModel = SubscriptionViewModel(subscriptions, FakeEntryRepository())

    viewModel.onSubscriptionPicked(uri = "content://picked")

    val added = subscriptions.storedSubscriptions.single()
    assertNull(added.expiresAtEpochMilli)
    assertNull(added.maxEntries)
    assertNull(added.price)
  }

  @Test
  fun onSetActive_makesThatSubscriptionActive() {
    val subscriptions = FakeSubscriptionRepository(listOf(older, newer), "new")
    val viewModel = SubscriptionViewModel(subscriptions, FakeEntryRepository())

    viewModel.onSetActive("old")

    assertEquals("old", subscriptions.storedActiveId)
    assertEquals(older, viewModel.activeSubscription.value)
  }

  @Test
  fun onDeleteSubscription_removesIt_andLeavesNoneActive_whenItWasActive() {
    val subscriptions = FakeSubscriptionRepository(listOf(older, newer), "new")
    val viewModel = SubscriptionViewModel(subscriptions, FakeEntryRepository())

    viewModel.onDeleteSubscription("new")

    assertEquals(listOf(older), viewModel.subscriptions.value)
    assertNull(viewModel.activeSubscription.value)
  }

  @Test
  fun onDeleteSubscription_keepsActiveSubscription_whenAnotherIsDeleted() {
    val subscriptions = FakeSubscriptionRepository(listOf(older, newer), "new")
    val viewModel = SubscriptionViewModel(subscriptions, FakeEntryRepository())

    viewModel.onDeleteSubscription("old")

    assertEquals(listOf(newer), viewModel.subscriptions.value)
    assertEquals(newer, viewModel.activeSubscription.value)
  }

  @Test
  fun onScannerAccepted_recordsOneEntryTaggedWithTheActiveSubscription() {
    val entries = FakeEntryRepository()
    val viewModel =
        SubscriptionViewModel(FakeSubscriptionRepository(listOf(older, newer), "new"), entries)
    val before = Instant.now().toEpochMilli()

    viewModel.onScannerAccepted()

    val after = Instant.now().toEpochMilli()
    val entry = entries.storedEntries.single()
    assertEquals("new", entry.subscriptionId)
    assertTrue(entry.timestampEpochMilli in before..after)
    assertEquals(
        Instant.ofEpochMilli(entry.timestampEpochMilli),
        viewModel.lastEntryTimestamp.value,
    )
  }

  @Test
  fun onScannerAccepted_recordsUntaggedEntry_whenNoSubscriptionIsActive() {
    val entries = FakeEntryRepository()
    val viewModel = SubscriptionViewModel(FakeSubscriptionRepository(listOf(older)), entries)

    viewModel.onScannerAccepted()

    assertNull(entries.storedEntries.single().subscriptionId)
  }
}
