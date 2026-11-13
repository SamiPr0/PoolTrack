package com.github.se.pooltrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolPaletteTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun deepWater_dependsOnTheTheme() {
    var light: List<Color> = emptyList()
    var dark: List<Color> = emptyList()
    var lightIsDark = true
    var darkIsDark = false
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) {
        light = deepWaterColors(MaterialTheme.colorScheme)
        lightIsDark = isDarkScheme(MaterialTheme.colorScheme)
      }
      PoolTrackTheme(darkTheme = true) {
        dark = deepWaterColors(MaterialTheme.colorScheme)
        darkIsDark = isDarkScheme(MaterialTheme.colorScheme)
      }
    }
    composeRule.waitForIdle()

    assertEquals(PoolPalette.DeepLight, light)
    assertEquals(PoolPalette.DeepDark, dark)
    assertFalse(lightIsDark)
    assertTrue(darkIsDark)
  }

  @Test
  fun poolTile_isWhiteInTheLightTheme_andLighterThanThePageInTheDarkTheme() {
    var light = Color.Unspecified
    var dark = Color.Unspecified
    var darkPage = Color.Unspecified
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) { light = poolTileColor(MaterialTheme.colorScheme) }
      PoolTrackTheme(darkTheme = true) {
        dark = poolTileColor(MaterialTheme.colorScheme)
        darkPage = MaterialTheme.colorScheme.background
      }
    }
    composeRule.waitForIdle()

    assertEquals(Color.White, light)
    assertTrue(dark.luminance() > darkPage.luminance())
  }
}
