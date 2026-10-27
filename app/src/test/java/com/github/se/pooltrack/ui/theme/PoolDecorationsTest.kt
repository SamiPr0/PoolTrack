package com.github.se.pooltrack.ui.theme

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolDecorationsTest {

  @Test
  fun depthMarkerLabel_startsAtOneMetre_andGoesHalfAMetreDeeper() {
    assertEquals(
        listOf("1.0 m", "1.5 m", "2.0 m", "2.5 m"),
        (0..3).map { depthMarkerLabel(it) },
    )
  }

  @Test
  fun depthMarkerLabel_treatsNegativeIndexAsFirst() {
    assertEquals("1.0 m", depthMarkerLabel(-2))
  }

  @Test
  fun waterSurfaceY_isMeasuredFromTheTop() {
    assertEquals(200f, waterSurfaceY(level = 0f, height = 200f), 0f)
    assertEquals(50f, waterSurfaceY(level = 0.75f, height = 200f), 0f)
    assertEquals(0f, waterSurfaceY(level = 1f, height = 200f), 0f)
  }

  @Test
  fun waterSurfaceY_clampsLevelOutsideZeroToOne() {
    assertEquals(200f, waterSurfaceY(level = -0.5f, height = 200f), 0f)
    assertEquals(0f, waterSurfaceY(level = 3f, height = 200f), 0f)
  }

  @Test
  fun waterPath_isEmpty_withoutWaterOrSpace() {
    assertTrue(waterPath(Size(100f, 100f), 0f, 4f, 40f).isEmpty)
    assertTrue(waterPath(Size(0f, 100f), 0.5f, 4f, 40f).isEmpty)
    assertTrue(waterPath(Size(100f, 0f), 0.5f, 4f, 40f).isEmpty)
  }

  @Test
  fun waterPath_coversTheWidth_fromItsSurfaceDownToTheBottom() {
    val bounds = waterPath(Size(200f, 100f), 0.4f, 5f, 40f).getBounds()
    assertEquals(0f, bounds.left, 0f)
    assertEquals(200f, bounds.right, 0.5f)
    assertEquals(100f, bounds.bottom, 0f)
    // The surface sits at 60, its waves reaching up to half the amplitude above it.
    assertTrue(bounds.top in 55f..60f)
  }

  @Test
  fun waterPath_keepsAWave_whenFull() {
    val bounds = waterPath(Size(200f, 100f), 1f, 5f, 40f).getBounds()
    assertTrue(bounds.top >= 0f)
    assertEquals(100f, bounds.bottom, 0f)
  }
}
