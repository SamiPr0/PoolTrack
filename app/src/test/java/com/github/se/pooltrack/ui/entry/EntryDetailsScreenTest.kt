package com.github.se.pooltrack.ui.entry

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.NavigationTestTags
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryDetailsScreenTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  @get:Rule val composeTestRule = createComposeRule()

  private val subscription =
      Subscription(
          id = "sub-1",
          uri = "content://pass",
          displayName = "Pool 10x",
          addedAtEpochMilli = 0L,
          maxEntries = 10,
          price = 50.0,
      )
  // Fixed, long-past times: a swim without a duration is "Not recorded", never "In progress".
  private val first = Entry(1_000_000_000_000L, subscriptionId = "sub-1")
  private val second =
      Entry(1_000_259_200_000L, subscriptionId = "sub-1", swimDurationMillis = 4_320_000L)

  private fun show(
      timestamp: Long,
      entries: FakeEntryRepository = FakeEntryRepository(listOf(first, second)),
      subscriptions: List<Subscription> = listOf(subscription),
      navigationActions: NavigationActions? = null,
  ) {
    val viewModel = EntryDetailsViewModel(entries, FakeSubscriptionRepository(subscriptions))
    composeTestRule.setContent { EntryDetailsScreen(timestamp, viewModel, navigationActions) }
  }

  @Test
  fun entryDetailsScreen_showsEveryDetail_whenEntryExists() {
    show(second.timestampEpochMilli)

    val date =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(second.timestampEpochMilli))
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextContains("Swim")
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DATE).assertTextContains(date)
    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SWIM_DURATION)
        .assertTextContains("1h 12min")
    composeTestRule.onNodeWithText("in the water").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.SWIM_NUMBER).assertIsDisplayed()
    composeTestRule.onNodeWithText("#2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pool 10x · entry 2 of 10").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.COST).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(String.format(Locale.getDefault(), "%.2f", 5.0))
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("3 days").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun entryDetailsScreen_showsFallbacks_whenDataIsMissing() {
    show(first.timestampEpochMilli, subscriptions = emptyList())

    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SWIM_DURATION)
        .assertTextContains("Not recorded")
    composeTestRule.onNodeWithText("Deleted subscription").assertIsDisplayed()
    composeTestRule.onNodeWithText("First swim").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(EntryDetailsScreenTestTags.COST).assertCountEquals(0)
  }

  @Test
  fun entryDetailsScreen_showsSwimInProgress_whenEntryIsRecentAndNotReported() {
    val now = Entry(Instant.now().toEpochMilli(), subscriptionId = "sub-1")
    show(now.timestampEpochMilli, entries = FakeEntryRepository(listOf(now)))

    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SWIM_DURATION)
        .assertTextContains("In progress")
  }

  @Test
  fun entryDetailsScreen_omitsTheEntryNumberLimit_whenSubscriptionHasNone() {
    show(first.timestampEpochMilli, subscriptions = listOf(subscription.copy(maxEntries = null)))

    composeTestRule.onNodeWithText("Pool 10x · entry 1").assertIsDisplayed()
  }

  @Test
  fun entryDetailsScreen_showsNotFound_andNoDeleteButton_whenEntryDoesNotExist() {
    show(42L)

    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.NOT_FOUND_MESSAGE).assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(EntryDetailsScreenTestTags.DELETE_BUTTON).assertCountEquals(0)
  }

  @Test
  fun entryDetailsScreen_deletesTheEntryAndGoesBack_whenDeleteConfirmed() {
    val repository = FakeEntryRepository(listOf(first, second))
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    show(second.timestampEpochMilli, entries = repository, navigationActions = navigationActions)

    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DELETE_BUTTON).performClick()
    composeTestRule.onNodeWithText("Delete this swim?").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.CONFIRM_DELETE_BUTTON).performClick()

    assertEquals(listOf(first), repository.storedEntries)
    verify(exactly = 1) { navigationActions.goBack() }
  }

  @Test
  fun entryDetailsScreen_keepsTheEntry_whenDeleteCancelled() {
    val repository = FakeEntryRepository(listOf(first, second))
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    show(second.timestampEpochMilli, entries = repository, navigationActions = navigationActions)

    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DELETE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.CANCEL_DELETE_BUTTON).performClick()

    assertEquals(listOf(first, second), repository.storedEntries)
    composeTestRule.onAllNodesWithText("Delete this swim?").assertCountEquals(0)
    verify(exactly = 0) { navigationActions.goBack() }
  }

  @Test
  fun entryDetailsScreen_goesBack_whenBackTapped() {
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    show(second.timestampEpochMilli, navigationActions = navigationActions)

    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()

    verify(exactly = 1) { navigationActions.goBack() }
  }

  @Test
  fun entryDetailsViewModel_readsProviderRepositories_whenCreatedWithDefaults() {
    EntryRepositoryProvider.repository = FakeEntryRepository(listOf(first))
    SubscriptionRepositoryProvider.repository = FakeSubscriptionRepository(listOf(subscription))
    try {
      val viewModel = EntryDetailsViewModel()
      viewModel.loadEntry(first.timestampEpochMilli)

      assertEquals(first, viewModel.details.value?.entry)
      assertEquals(subscription, viewModel.details.value?.subscription)
    } finally {
      // The providers are lateinit singletons: un-initialise them so later tests are unaffected.
      listOf(EntryRepositoryProvider, SubscriptionRepositoryProvider).forEach { provider ->
        provider::class.java.getDeclaredField("repository").apply {
          isAccessible = true
          set(provider, null)
        }
      }
    }
  }
}
