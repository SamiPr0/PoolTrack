package com.github.se.pooltrack.ui.update

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.pooltrack.utils.FakeUpdateRepository
import com.github.se.pooltrack.utils.FakeUpdateRepository.Companion.APK
import com.github.se.pooltrack.utils.FakeUpdateRepository.Companion.UPDATE
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateDialogTest {

  @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
  @get:Rule(order = 1) val composeTestRule = createComposeRule()

  private lateinit var originalLocale: Locale
  private var confirmed = 0
  private var dismissed = 0

  @Before
  fun setUp() {
    originalLocale = Locale.getDefault()
    Locale.setDefault(Locale.US)
  }

  @After
  fun tearDown() {
    Locale.setDefault(originalLocale)
  }

  private fun show(state: UpdateUiState) {
    composeTestRule.setContent {
      UpdateDialogContent(state, onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
    }
  }

  private fun assertTagIsGone(tag: String) {
    composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
  }

  @Test
  fun dialog_showsNothing_whenThereIsNoUpdate() {
    show(UpdateUiState.None)

    assertTagIsGone(UpdateDialogTestTags.DIALOG_TITLE)
  }

  @Test
  fun dialog_offersTheUpdate_withItsVersionSizeAndNotes() {
    show(UpdateUiState.Available(UPDATE))

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Update available")
    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.VERSION)
        .assertTextEquals("Version 1.2.0 (12.3 MB)")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.NOTES).assertTextEquals("• Faster start")
    composeTestRule.onNodeWithText("Update").assertIsDisplayed()
    composeTestRule.onNodeWithText("Later").assertIsDisplayed()
  }

  @Test
  fun dialog_hidesTheSizeAndNotes_whenTheReleaseHasNone() {
    show(UpdateUiState.Available(UPDATE.copy(sizeBytes = 0, notes = "")))

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.VERSION).assertTextEquals("Version 1.2.0")
    assertTagIsGone(UpdateDialogTestTags.NOTES)
  }

  @Test
  fun dialog_callsBack_whenUpdateOrLaterIsTapped() {
    show(UpdateUiState.Available(UPDATE))

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.CONFIRM_BUTTON).performClick()
    assertEquals(1, confirmed)
    assertEquals(0, dismissed)

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.DISMISS_BUTTON).performClick()
    assertEquals(1, dismissed)
  }

  @Test
  fun dialog_showsTheDownloadPercentage_andOnlyOffersCancel() {
    show(UpdateUiState.Downloading(UPDATE, 0.42f))

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Downloading update")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.PROGRESS).assertIsDisplayed()
    composeTestRule.onNodeWithText("42%").assertIsDisplayed()
    assertTagIsGone(UpdateDialogTestTags.CONFIRM_BUTTON)

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.DISMISS_BUTTON).assertTextEquals("Cancel")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.DISMISS_BUTTON).performClick()
    assertEquals(1, dismissed)
  }

  @Test
  fun dialog_showsAnIndeterminateBar_untilTheSizeIsKnown() {
    show(UpdateUiState.Downloading(UPDATE, null))

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.PROGRESS).assertIsDisplayed()
    composeTestRule.onNodeWithText("Starting download…").assertIsDisplayed()
  }

  @Test
  fun dialog_tellsTheAppWillClose_whenReadyToInstall() {
    show(UpdateUiState.ReadyToInstall(UPDATE, APK))

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Ready to install")
    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.MESSAGE)
        .assertTextEquals("PoolTrack will close to finish the update. Open it again afterwards.")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.CONFIRM_BUTTON).assertTextEquals("Install")
  }

  @Test
  fun dialog_explainsHowToAllowInstalls_whenPermissionIsNeeded() {
    show(UpdateUiState.ReadyToInstall(UPDATE, APK, needsPermission = true))

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.MESSAGE)
        .assertTextEquals(
            "Allow PoolTrack to install apps in the settings that just opened, then come back here."
        )
  }

  @Test
  fun dialog_showsTheError_andOffersARetry() {
    show(UpdateUiState.Failed(UPDATE, "Checksum mismatch"))

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Update failed")
    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.MESSAGE)
        .assertTextEquals("Checksum mismatch")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.CONFIRM_BUTTON).assertTextEquals("Retry")
    composeTestRule.onNodeWithTag(UpdateDialogTestTags.DISMISS_BUTTON).assertTextEquals("Close")
  }

  @Test
  fun updateDialog_walksThroughDownloadAndInstall_withViewModel() {
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = UpdateViewModel(repository, "1.1.0")
    composeTestRule.setContent { UpdateDialog(viewModel) }

    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Update available")

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.CONFIRM_BUTTON).performClick()
    assertEquals(listOf(APK), repository.installedApks)
    composeTestRule
        .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
        .assertTextEquals("Ready to install")

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.CONFIRM_BUTTON).performClick()
    assertEquals(listOf(APK, APK), repository.installedApks)

    composeTestRule.onNodeWithTag(UpdateDialogTestTags.DISMISS_BUTTON).performClick()
    assertTagIsGone(UpdateDialogTestTags.DIALOG_TITLE)
  }

  @Test
  fun formatSize_showsMegabytesWithOneDecimal() {
    assertEquals("0.0 MB", formatSize(0))
    assertEquals("1.0 MB", formatSize(1_048_576))
    assertEquals("12.3 MB", formatSize(12_900_000))
  }

  @Test
  fun toPlainNotes_dropsMarkdownMarkers_andKeepsBullets() {
    val markdown = "## What's new\n- **Faster** start\n* Fix bug\n\n\n\nThanks"

    assertEquals("What's new\n• Faster start\n• Fix bug\n\nThanks", markdown.toPlainNotes())
  }

  @Test
  fun toPlainNotes_isEmpty_forBlankNotes() {
    assertEquals("", " \n ".toPlainNotes())
  }
}
