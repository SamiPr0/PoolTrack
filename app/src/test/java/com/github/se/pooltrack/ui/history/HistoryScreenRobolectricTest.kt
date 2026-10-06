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
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.NavigationTestTags
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
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
    val viewModel = HistoryViewModel(repository, FakeSubscriptionRepository())
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
  fun historyScreen_showsSwimDurationNextToTime_whenRecorded() {
    val reported = entryAt(today, hour = 8).copy(swimDurationMillis = 4_320_000L)
    val unreported = entryAt(today.minusDays(1), hour = 9)
    show(FakeEntryRepository(listOf(reported, unreported)))

    composeTestRule.onNodeWithText("${timeLabel(reported)} · 1h 12min").assertIsDisplayed()
    composeTestRule.onNodeWithText(timeLabel(unreported)).assertIsDisplayed()
  }

  @Test
  fun historyScreen_opensEntryDetails_whenEntryTapped() {
    val entry = entryAt(today)
    val repository = FakeEntryRepository(listOf(entry))
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    show(repository, navigationActions)

    composeTestRule.onNodeWithText(timeLabel(entry)).performClick()

    verify(exactly = 1) { navigationActions.navigateToEntryDetails(entry.timestampEpochMilli) }
    assertEquals(listOf(entry), repository.storedEntries)
  }

  @Test
  fun historyScreen_ignoresEntryTap_whenNoNavigationActions() {
    val entry = entryAt(today)
    show(FakeEntryRepository(listOf(entry)))

    composeTestRule.onNodeWithText(timeLabel(entry)).performClick()

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
    SubscriptionRepositoryProvider.repository = FakeSubscriptionRepository()
    try {
      assertEquals(listOf(entry), HistoryViewModel().entries.value)
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

  private fun addYesterdayAtNoon() {
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.ADD_PAST_ENTRY_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.YESTERDAY_CHIP).performClick()
    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.ADD_BUTTON).performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun historyScreen_opensTheAddSheet_whenTheAddButtonIsTapped() {
    show(FakeEntryRepository())

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.ADD_PAST_ENTRY_BUTTON).performClick()

    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.SHEET).assertIsDisplayed()
  }

  @Test
  fun historyScreen_addsAPastEntryAndClosesTheSheet_withAnUndoSnackbar() {
    val repository = FakeEntryRepository()
    show(repository)

    addYesterdayAtNoon()

    val expected = today.minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    assertEquals(listOf(expected), repository.storedEntries.map { it.timestampEpochMilli })
    composeTestRule.onAllNodesWithTag(AddPastEntrySheetTestTags.SHEET).assertCountEquals(0)
    composeTestRule.onNodeWithText("Entry added").assertIsDisplayed()
  }

  @Test
  fun historyScreen_removesTheEntry_whenUndoIsTapped() {
    val repository = FakeEntryRepository()
    show(repository)
    addYesterdayAtNoon()

    composeTestRule.onNodeWithText(HistoryScreenTestTags.UNDO_ADD_ACTION).performClick()
    composeTestRule.waitForIdle()

    assertEquals(emptyList<Entry>(), repository.storedEntries)
  }

  @Test
  fun historyScreen_showsAnInlineError_whenTheEntryAlreadyExists() {
    val repository = FakeEntryRepository()
    show(repository)
    addYesterdayAtNoon()

    // The sheet remembers the day and time just added, so adding again hits the same instant.
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.ADD_PAST_ENTRY_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.ADD_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddPastEntrySheetTestTags.ERROR).assertIsDisplayed()
    assertEquals(1, repository.storedEntries.size)
  }

  private val expiredPass =
      Subscription(
          id = "pass",
          uri = "content://pass",
          displayName = "pass",
          addedAtEpochMilli = System.currentTimeMillis(),
          expiresAtEpochMilli = Instant.now().minusSeconds(86_400).toEpochMilli(),
      )

  @Test
  fun historyScreen_offersToLinkUnlinkedEntries_andLinksThemWithUndo() {
    val unlinked = Entry(entryAt(today.minusDays(30)).timestampEpochMilli, subscriptionId = null)
    val repository = FakeEntryRepository(listOf(unlinked))
    val viewModel = HistoryViewModel(repository, FakeSubscriptionRepository(listOf(expiredPass)))
    composeTestRule.setContent { HistoryScreen(viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.LINK_BANNER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.LINK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals("pass", repository.storedEntries.single().subscriptionId)
    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.LINK_BANNER).assertCountEquals(0)
    composeTestRule.onNodeWithText("1 entry linked").assertIsDisplayed()

    composeTestRule.onNodeWithText(HistoryScreenTestTags.UNDO_ADD_ACTION).performClick()
    composeTestRule.waitForIdle()
    assertEquals(null, repository.storedEntries.single().subscriptionId)
  }

  @Test
  fun historyScreen_hidesTheLinkBanner_whenNothingCanBeLinked() {
    show(FakeEntryRepository(listOf(Entry(entryAt(today).timestampEpochMilli))))

    composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.LINK_BANNER).assertCountEquals(0)
  }
}
