package com.github.se.pooltrack.ui.entry

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryDetailsEditScreenTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  @get:Rule val composeTestRule = createComposeRule()

  private val first = Entry(1_000_000_000_000L, subscriptionId = "sub-1")
  private val second =
      Entry(1_000_259_200_000L, subscriptionId = "sub-1", swimDistanceMeters = 1200)

  private fun show(timestamp: Long, repository: FakeEntryRepository) {
    val viewModel = EntryDetailsViewModel(repository, FakeSubscriptionRepository())
    composeTestRule.setContent { EntryDetailsScreen(timestamp, viewModel) }
  }

  private fun openEditor() {
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.EDIT_BUTTON).performClick()
  }

  @Test
  fun editingTheDistance_savesTheNewValue() {
    val repository = FakeEntryRepository(listOf(first, second))
    show(second.timestampEpochMilli, repository)
    openEditor()
    composeTestRule.onNodeWithText("Edit distance").assertIsDisplayed()

    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD)
        .performTextInput("1500")
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.SAVE_DISTANCE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals(1500, repository.storedEntries.last().swimDistanceMeters)
    composeTestRule.onAllNodesWithText("Edit distance").assertCountEquals(0)
    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SWIM_DISTANCE)
        .assertTextContains("1500 m")
  }

  @Test
  fun editing_startsFromTheLoggedDistance() {
    show(second.timestampEpochMilli, FakeEntryRepository(listOf(first, second)))

    openEditor()

    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD)
        .assertTextContains("1200")
  }

  @Test
  fun anInvalidDistance_cannotBeSaved() {
    show(first.timestampEpochMilli, FakeEntryRepository(listOf(first, second)))
    openEditor()

    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SAVE_DISTANCE_BUTTON)
        .assertIsNotEnabled()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD).performTextInput("0")
    composeTestRule
        .onNodeWithTag(EntryDetailsScreenTestTags.SAVE_DISTANCE_BUTTON)
        .assertIsNotEnabled()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD).performTextInput("800")
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.SAVE_DISTANCE_BUTTON).assertIsEnabled()
  }

  @Test
  fun cancellingTheEdit_keepsTheDistance() {
    val repository = FakeEntryRepository(listOf(first, second))
    show(second.timestampEpochMilli, repository)
    openEditor()
    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.DISTANCE_FIELD).performTextInput("9")

    composeTestRule.onNodeWithTag(EntryDetailsScreenTestTags.CANCEL_EDIT_BUTTON).performClick()

    assertEquals(1200, repository.storedEntries.last().swimDistanceMeters)
    composeTestRule.onAllNodesWithText("Edit distance").assertCountEquals(0)
  }

  @Test
  fun theEditButton_isHidden_whenTheEntryDoesNotExist() {
    show(42L, FakeEntryRepository(listOf(first, second)))

    composeTestRule.onAllNodesWithTag(EntryDetailsScreenTestTags.EDIT_BUTTON).assertCountEquals(0)
  }
}
