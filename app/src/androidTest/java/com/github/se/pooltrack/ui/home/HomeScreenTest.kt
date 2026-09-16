package com.github.se.pooltrack.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun homeScreen_displaysHelloWorld() {
    composeTestRule.setContent { HomeScreen() }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.GREETING).assertIsDisplayed()
    composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
  }
}
