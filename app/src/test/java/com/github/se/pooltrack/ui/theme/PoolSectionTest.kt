package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolSectionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun sectionHeader_showsItsTitle_andItsDetailWhenGiven() {
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) {
        Column {
          SectionHeader("ACTIVITY")
          SectionHeader("This week", detail = "2 visits · 3.0 km")
        }
      }
    }

    composeRule.onNodeWithText("ACTIVITY").assertIsDisplayed()
    composeRule.onNodeWithText("This week").assertIsDisplayed()
    composeRule.onNodeWithText("2 visits · 3.0 km").assertIsDisplayed()
  }
}
