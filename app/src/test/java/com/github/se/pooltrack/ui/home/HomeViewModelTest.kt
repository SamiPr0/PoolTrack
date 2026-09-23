package com.github.se.pooltrack.ui.home

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val passA =
      Subscription(id = "a", uri = "content://a", displayName = "A", addedAtEpochMilli = 1L)
  private val passB =
      Subscription(
          id = "b",
          uri = "content://b",
          displayName = "B",
          addedAtEpochMilli = 2L,
          price = 60.0,
          maxEntries = 10,
      )
  private val entryA = Entry(timestampEpochMilli = 1_000L, subscriptionId = "a")
  private val entryB1 = Entry(timestampEpochMilli = 2_000L, subscriptionId = "b")
  private val entryB2 = Entry(timestampEpochMilli = 3_000L, subscriptionId = "b")
  private val untagged = Entry(timestampEpochMilli = 500L, subscriptionId = null)

  private fun viewModel(
      entries: FakeEntryRepository = FakeEntryRepository(),
      subscriptions: FakeSubscriptionRepository = FakeSubscriptionRepository(),
  ) = HomeViewModel(entries, subscriptions)

  @Test
  fun stats_countsNoEntries_whenNoneRecorded() {
    val stats = viewModel().stats.value

    assertEquals(0, stats.totalEntries)
    assertNull(stats.daysSinceLastSwim)
    assertNull(stats.averageEntriesPerWeek)
    assertNull(stats.favoriteDayOfWeek)
  }

  @Test
  fun stats_countsEveryEntry_whenEntriesRecorded() {
    val stats = viewModel(FakeEntryRepository(listOf(entryA, entryB1, untagged))).stats.value

    assertEquals(3, stats.totalEntries)
  }

  @Test
  fun stats_updates_whenAnEntryIsRecorded() = runTest {
    val entries = FakeEntryRepository(listOf(entryA))
    val viewModel = viewModel(entries)

    entries.addEntry(entryB1)

    assertEquals(2, viewModel.stats.value.totalEntries)
  }

  @Test
  fun lastEntryTimestamp_isMostRecentEntry_whenEntriesRecorded() {
    val viewModel = viewModel(FakeEntryRepository(listOf(entryB2, entryA, entryB1)))

    assertEquals(Instant.ofEpochMilli(3_000L), viewModel.lastEntryTimestamp.value)
  }

  @Test
  fun lastEntryTimestamp_isNull_whenNoEntriesRecorded() {
    assertNull(viewModel().lastEntryTimestamp.value)
  }

  @Test
  fun activeSubscription_isTheActiveOne_whenOneIsActive() {
    val viewModel = viewModel(subscriptions = FakeSubscriptionRepository(listOf(passA, passB), "b"))

    assertEquals(passB, viewModel.activeSubscription.value)
  }

  @Test
  fun activeSubscription_isNull_whenNoneIsActive() {
    val viewModel = viewModel(subscriptions = FakeSubscriptionRepository(listOf(passA, passB)))

    assertNull(viewModel.activeSubscription.value)
  }

  @Test
  fun totalSpent_sumsRecordedPrices_ignoringSubscriptionsWithoutOne() {
    val passC = passB.copy(id = "c", price = 12.5)
    val viewModel =
        viewModel(subscriptions = FakeSubscriptionRepository(listOf(passA, passB, passC)))

    assertEquals(72.5, viewModel.totalSpent.value, 0.0)
  }

  @Test
  fun totalSpent_isZero_whenNoSubscriptions() {
    assertEquals(0.0, viewModel().totalSpent.value, 0.0)
  }

  @Test
  fun activeSubscriptionUsedEntries_countsOnlyEntriesOfTheActiveSubscription() {
    val viewModel =
        viewModel(
            FakeEntryRepository(listOf(entryA, entryB1, entryB2, untagged)),
            FakeSubscriptionRepository(listOf(passA, passB), "b"),
        )

    assertEquals(2, viewModel.activeSubscriptionUsedEntries.value)
  }

  @Test
  fun activeSubscriptionUsedEntries_isZero_whenNoneIsActive() {
    val viewModel =
        viewModel(
            FakeEntryRepository(listOf(entryA, entryB1, untagged)),
            FakeSubscriptionRepository(listOf(passA, passB)),
        )

    assertEquals(0, viewModel.activeSubscriptionUsedEntries.value)
  }

  @Test
  fun activeSubscriptionUsedEntries_updates_whenAnotherSubscriptionIsActivated() = runTest {
    val subscriptions = FakeSubscriptionRepository(listOf(passA, passB), "b")
    val viewModel = viewModel(FakeEntryRepository(listOf(entryA, entryB1, entryB2)), subscriptions)

    subscriptions.setActiveSubscription("a")

    assertEquals(1, viewModel.activeSubscriptionUsedEntries.value)
  }
}
