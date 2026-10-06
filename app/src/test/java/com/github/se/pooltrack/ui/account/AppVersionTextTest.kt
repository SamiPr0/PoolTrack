package com.github.se.pooltrack.ui.account

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.se.pooltrack.BuildConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppVersionTextTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun appVersionText_showsTheGivenVersion() {
    composeRule.setContent { AppVersionText(version = "9.8.7") }

    composeRule.onNodeWithTag(AppVersionTextTestTags.VERSION).assertTextEquals("Version 9.8.7")
  }

  @Test
  fun appVersionText_defaultsToTheBuildVersion() {
    composeRule.setContent { AppVersionText() }

    composeRule
        .onNodeWithTag(AppVersionTextTestTags.VERSION)
        .assertTextEquals("Version ${BuildConfig.VERSION_NAME}")
  }
}
