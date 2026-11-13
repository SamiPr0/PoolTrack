package com.github.se.pooltrack.ui.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
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

  private val resolver
    get() = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

  private fun setAnimatorScale(scale: Float) {
    Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
  }

  @Test
  fun animationsEnabled_followsTheSystemAnimatorScale() {
    setAnimatorScale(1f)
    assertTrue(animationsEnabled(resolver))

    setAnimatorScale(0f)
    assertFalse(animationsEnabled(resolver))
  }

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
  fun deepWater_drawsItsContentInTheOnDeepColor() {
    var contentColor = Color.Unspecified
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) {
        DeepWater(shape = RoundedCornerShape(20.dp)) {
          contentColor = LocalContentColor.current
          Text("Deep")
        }
      }
    }

    composeRule.onNodeWithText("Deep").assertExists()
    assertEquals(PoolPalette.OnDeep, contentColor)
  }
}
