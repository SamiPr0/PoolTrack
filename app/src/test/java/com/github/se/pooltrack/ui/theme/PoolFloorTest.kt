package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolFloorTest {

  @Test
  fun deepestMark_isTheDeepestWater_inTheLightTheme() {
    assertEquals(PoolPalette.DeepLight.last(), deepestMarkColor(lightColorScheme()))
  }

  @Test
  fun deepestMark_staysLightEnoughToShow_inTheDarkTheme() {
    val dark = darkColorScheme(background = Color(0xFF062B3D))

    assertEquals(PoolPalette.DeepDark.first(), deepestMarkColor(dark))
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
}
