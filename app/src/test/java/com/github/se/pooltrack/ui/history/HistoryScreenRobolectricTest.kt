package com.github.se.pooltrack.ui.history

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.NavigationTestTags
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryScreenRobolectricTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  @get:Rule val composeTestRule = createComposeRule()

  private val zone = ZoneId.systemDefault()
  private val today = LocalDate.now(zone)

  private fun entryAt(day: LocalDate, hour: Int = 12, minute: Int = 0) =
      Entry(
          timestampEpochMilli = day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli(),
          subscriptionId = "sub-1",
      )

  private fun show(repository: FakeEntryRepository, navigationActions: NavigationActions? = null) {
    val viewModel = HistoryViewModel(repository)
    composeTestRule.setContent { HistoryScreen(viewModel, navigationActions) }
  }

  private fun timeLabel(entry: Entry): String =
      DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
          .withLocale(Locale.getDefault())
          .withZone(zone)
          .format(Instant.ofEpochMilli(entry.timestampEpochMilli))

  @Test
  fun historyScreen_showsEmptyState_whenNoEntriesRecorded() {
    show(FakeEntryRepository())

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No entries yet").assertIsDisplayed()
    composeTestRule.onNodeWithText("Confirmed pool visits will show up here.").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.ENTRY_LIST).assertCountEquals(0)
  }

  @Test
  fun historyScreen_showsEntriesGroupedByDay_whenEntriesRecorded() {
    val yesterdayEntry = entryAt(today.minusDays(1), hour = 9)
    val todayMorning = entryAt(today, hour = 8)
    val todayEvening = entryAt(today, hour = 18)
    show(FakeEntryRepository(listOf(yesterdayEntry, todayMorning, todayEvening)))

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.ENTRY_LIST).assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertCountEquals(0)
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.DAY_HEADER).assertCountEquals(2)
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.ENTRY_ITEM).assertCountEquals(3)
    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
    composeTestRule.onNodeWithText("2 entries").assertIsDisplayed()
    composeTestRule.onNodeWithText("1 entry").assertIsDisplayed()
    composeTestRule.onNodeWithText(timeLabel(todayMorning)).assertIsDisplayed()
    composeTestRule.onNodeWithText(timeLabel(todayEvening)).assertIsDisplayed()
    composeTestRule.onNodeWithText(timeLabel(yesterdayEntry)).assertIsDisplayed()
  }

  @Test
  fun historyScreen_labelsOlderDaysWithoutYear_whenSameYear() {
    val sameYearDay = listOf(today.minusDays(2), today.plusDays(2)).first { it.year == today.year }
    show(FakeEntryRepository(listOf(entryAt(sameYearDay))))

    val expected =
        sameYearDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    composeTestRule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun historyScreen_labelsOlderDaysWithYear_whenDifferentYear() {
    val lastYearDay = today.minusYears(1)
    show(FakeEntryRepository(listOf(entryAt(lastYearDay))))

    val expected =
        lastYearDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
    composeTestRule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun historyScreen_showsConfirmationDialog_whenDeleteTapped() {
    val entry = entryAt(today)
    val repository = FakeEntryRepository(listOf(entry))
    show(repository)

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.DELETE_BUTTON).performClick()

    composeTestRule.onNodeWithText("Delete this entry?").assertIsDisplayed()
    composeTestRule.onNode(hasText("permanently removed.", substring = true)).assertIsDisplayed()
    assertEquals(listOf(entry), repository.storedEntries)
  }

  @Test
  fun historyScreen_deletesOnlyThatEntry_whenDeleteConfirmed() {
    val keep = entryAt(today, hour = 8)
    val remove = entryAt(today, hour = 18)
    val repository = FakeEntryRepository(listOf(keep, remove))
    show(repository)

    // Most recent first, so the first delete button belongs to the 18:00 entry.
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.DELETE_BUTTON).onFirst().performClick()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.CONFIRM_DELETE_BUTTON).performClick()

    assertEquals(listOf(keep), repository.storedEntries)
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.ENTRY_ITEM).assertCountEquals(1)
    composeTestRule.onAllNodesWithText("Delete this entry?").assertCountEquals(0)
  }

  @Test
  fun historyScreen_showsEmptyState_whenLastEntryDeleted() {
    val repository = FakeEntryRepository(listOf(entryAt(today)))
    show(repository)

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.DELETE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.CONFIRM_DELETE_BUTTON).performClick()

    assertEquals(emptyList<Entry>(), repository.storedEntries)
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun historyScreen_keepsEntry_whenDeleteCancelled() {
    val entry = entryAt(today)
    val repository = FakeEntryRepository(listOf(entry))
    show(repository)

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.DELETE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.CANCEL_DELETE_BUTTON).performClick()

    assertEquals(listOf(entry), repository.storedEntries)
    composeTestRule.onAllNodesWithText("Delete this entry?").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.ENTRY_ITEM).assertCountEquals(1)
  }

  @Test
  fun historyScreen_showsNewEntry_whenRepositoryChangesAfterComposition() {
    val repository = FakeEntryRepository()
    show(repository)
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()

    runBlocking { repository.addEntry(entryAt(today)) }
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.ENTRY_ITEM).assertCountEquals(1)
  }

  @Test
  fun historyScreen_navigatesToTappedTab_whenBottomTabSelected() {
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    show(FakeEntryRepository(), navigationActions)

    composeTestRule.onNodeWithTag(NavigationTestTags.HOME_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.SUBSCRIPTION_TAB).performClick()

    verify(exactly = 1) { navigationActions.navigateTo(Screen.Home) }
    verify(exactly = 1) { navigationActions.navigateTo(Screen.Subscription) }
  }

  @Test
  fun historyScreen_ignoresTabTap_whenNoNavigationActions() {
    show(FakeEntryRepository())

    composeTestRule.onNodeWithTag(NavigationTestTags.HOME_TAB).performClick()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun historyViewModel_readsProviderRepository_whenCreatedWithDefaults() {
    val entry = entryAt(today)
    EntryRepositoryProvider.repository = FakeEntryRepository(listOf(entry))
    try {
      assertEquals(listOf(entry), HistoryViewModel().entries.value)
    } finally {
      // The provider is a lateinit singleton: un-initialise it so later tests are unaffected.
      EntryRepositoryProvider::class.java.getDeclaredField("repository").apply {
        isAccessible = true
        set(EntryRepositoryProvider, null)
      }
    }
  }
}
