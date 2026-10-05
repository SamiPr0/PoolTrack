package com.github.se.pooltrack.ui.history

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.platform.app.InstrumentationRegistry
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val application =
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

  @Before
  fun clearStoredEntries() {
    application.preferencesDataStoreFile("entry_prefs").delete()
  }

  @Test
  fun historyScreen_showsEmptyMessage_whenNoEntriesRecorded() {
    val viewModel =
        HistoryViewModel(EntryRepositoryLocal(application, currentUid = flowOf("test-user")))
    composeTestRule.setContent { HistoryScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }
}
