package com.github.se.pooltrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PoolDecorTest {

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
}
