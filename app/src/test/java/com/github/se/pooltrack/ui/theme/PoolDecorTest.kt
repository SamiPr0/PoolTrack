package com.github.se.pooltrack.ui.theme

import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
  fun laneFloatAt_putsRedFloatsAtBothEnds() {
    val rope = (0 until 10).map { laneFloatAt(it, 10) }

    assertEquals(List(3) { LaneFloat.End }, rope.take(3))
    assertEquals(List(3) { LaneFloat.End }, rope.takeLast(3))
  }

  @Test
  fun laneFloatAt_alternatesBlueAndWhiteInTheMiddle() {
    val middle = (3 until 7).map { laneFloatAt(it, 10) }

    assertEquals(listOf(LaneFloat.White, LaneFloat.Blue, LaneFloat.White, LaneFloat.Blue), middle)
  }

  @Test
  fun laneFloatColors_givesEachKindItsOwnColors() {
    assertEquals(PoolPalette.FloatRed, laneFloatColors(LaneFloat.End))
    assertEquals(PoolPalette.FloatBlue, laneFloatColors(LaneFloat.Blue))
    assertEquals(PoolPalette.FloatWhite, laneFloatColors(LaneFloat.White))
  }

  @Test
  fun depthLabel_writesOneDecimalAndTheUnit() {
    assertEquals("0.8 m", depthLabel(0.8))
    assertEquals("2.0 m", depthLabel(2.0))
  }

  @Test
  fun poolDepths_goFromShallowToDeep() {
    assertEquals(PoolDepths.sorted(), PoolDepths)
    assertEquals(5, PoolDepths.size)
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
