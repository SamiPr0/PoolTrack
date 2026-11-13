package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.se.pooltrack.ui.theme.PoolTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2000dp")
class StreakBannerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun streakTier_countsTheTiersReached() {
    assertEquals(0, streakTier(0))
    assertEquals(1, streakTier(1))
    assertEquals(1, streakTier(4))
    assertEquals(2, streakTier(5))
    assertEquals(3, streakTier(10))
    assertEquals(4, streakTier(20))
    assertEquals(5, streakTier(50))
    assertEquals(6, streakTier(75))
    assertEquals(7, streakTier(100))
    assertEquals(7, streakTier(400))
  }

  @Test
  fun everyTierHasALook() {
    assertEquals(STREAK_TIERS.size + 1, STREAK_LOOKS.size)
  }

  @Test
  fun streakHint_explainsWhatComesNext() {
    assertEquals("Swim this week to light it", streakHint(0))
    assertEquals("4 more to the next flame", streakHint(1))
    assertEquals("1 more to the next flame", streakHint(99))
    assertEquals("Top flame reached", streakHint(100))
  }

  @Test
  fun streakBanner_showsTheWeeksAndTheHint_atEveryTier() {
    val weeks = listOf(0, 3, 5, 10, 20, 50, 75, 100)
    composeRule.setContent {
      PoolTrackTheme(darkTheme = true) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          weeks.forEach { StreakBanner(weeks = it) }
        }
      }
    }
    composeRule.mainClock.advanceTimeBy(2_000)

    composeRule.onNodeWithText("Swim this week to light it").assertIsDisplayed()
    composeRule.onNodeWithText("Top flame reached").assertIsDisplayed()
    composeRule.onNodeWithText("75").assertIsDisplayed()
  }
}
