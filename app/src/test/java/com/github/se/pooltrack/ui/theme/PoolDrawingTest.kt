package com.github.se.pooltrack.ui.theme

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Draws the pool decorations, so their drawing code runs in every state. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PoolDrawingTest {

  @get:Rule val composeRule = createComposeRule()

  private val resolver
    get() = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

  private fun setAnimatorScale(scale: Float) {
    Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
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

  /** Draws every decoration once, so their drawing code runs, with [animatorScale] set. */
  private fun drawAllDecorations(animatorScale: Float, darkTheme: Boolean) {
    setAnimatorScale(animatorScale)
    composeRule.setContent {
      PoolTrackTheme(darkTheme = darkTheme) {
        Box(Modifier.size(300.dp, 900.dp).poolTiles(MaterialTheme.colorScheme).testTag("all")) {
          Column {
            DeepWater(Modifier.size(200.dp, 80.dp)) {}
            DeepWater(Modifier.size(200.dp, 40.dp), caustics = false) {}
            LaneRope(Modifier, animated = true)
            LaneRope(Modifier, animated = false)
            WaterFill(0.6f, PoolPalette.Water, Modifier.size(200.dp, 60.dp))
            WaterFill(2f, PoolPalette.WaterUrgent, Modifier.size(200.dp, 60.dp))
            Box(Modifier.size(150.dp, 50.dp)) { RippleRings(Modifier.fillMaxSize()) }
            Bubbles(Modifier.size(200.dp, 120.dp))
            LaneMarker(Modifier.size(60.dp, 120.dp))
            DepthGauge(Modifier.width(14.dp).height(200.dp))
          }
        }
      }
    }
    composeRule.mainClock.advanceTimeBy(1_000)
    val image = composeRule.onNodeWithTag("all").captureToImage()
    assertEquals(300f, image.width / composeRule.density.density, 1f)
  }

  /**
   * Recomposes every decoration, first with the same arguments (so Compose may skip them) and then
   * with new ones (so it must redraw them), and draws each state.
   */
  @Test
  fun decorations_redraw_whenTheirArgumentsChange_andSkip_whenTheyDoNot() {
    setAnimatorScale(0f)
    var tick by mutableIntStateOf(0)
    var variant by mutableIntStateOf(0)
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) {
        @Suppress("UNUSED_EXPRESSION") tick
        val second = variant == 1
        val size = if (second) Modifier.size(120.dp, 60.dp) else Modifier.size(100.dp, 50.dp)
        Column(Modifier.testTag("redraw")) {
          Caustics(size)
          LaneRope(size, animated = second)
          DepthGauge(size)
          WaterFill(
              if (second) 0.2f else 0.8f,
              if (second) PoolPalette.WaterUrgent else PoolPalette.Water,
              size,
          )
          RippleRings(size)
          Bubbles(size)
          LaneMarker(size)
        }
      }
    }
    composeRule.onNodeWithTag("redraw").captureToImage()

    tick++
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("redraw").captureToImage()

    variant = 1
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("redraw").captureToImage()
  }

  @Test
  fun decorations_drawWithAnimationsInTheLightTheme() {
    drawAllDecorations(animatorScale = 1f, darkTheme = false)
  }

  @Test
  fun decorations_drawStillWhenAnimationsAreOffInTheDarkTheme() {
    drawAllDecorations(animatorScale = 0f, darkTheme = true)
  }
}
