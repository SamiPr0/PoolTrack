package com.github.se.pooltrack.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolElementsTest {

  @Test
  fun laneFloatColor_isRedNearBothWalls_andBlueWhiteBetween() {
    val colors = (0 until 10).map { laneFloatColor(it, 10) }

    assertEquals(
        listOf(
            FloatColor.RED,
            FloatColor.RED,
            FloatColor.RED,
            FloatColor.WHITE,
            FloatColor.BLUE,
            FloatColor.WHITE,
            FloatColor.BLUE,
            FloatColor.RED,
            FloatColor.RED,
            FloatColor.RED,
        ),
        colors,
    )
  }

  @Test
  fun laneFloatColor_isAllRed_forAShortRope() {
    assertTrue((0 until 5).all { laneFloatColor(it, 5) == FloatColor.RED })
  }

  @Test
  fun entryFloats_marksUsedEntriesFirst_thenTheAvailableOnes() {
    assertEquals(
        listOf(false, false, false, true, true),
        entryFloats(usedCount = 3, maxEntries = 5),
    )
  }

  @Test
  fun entryFloats_clampsTheUsedCount() {
    assertEquals(List(4) { true }, entryFloats(usedCount = -2, maxEntries = 4))
    assertEquals(List(4) { false }, entryFloats(usedCount = 9, maxEntries = 4))
  }

  @Test
  fun entryFloats_isEmpty_withoutEntriesOrForBigPasses() {
    assertEquals(emptyList<Boolean>(), entryFloats(usedCount = 0, maxEntries = 0))
    assertEquals(MAX_ENTRY_FLOATS, entryFloats(0, MAX_ENTRY_FLOATS).size)
    assertEquals(
        emptyList<Boolean>(),
        entryFloats(usedCount = 0, maxEntries = MAX_ENTRY_FLOATS + 1),
    )
  }

  @Test
  fun waterColumnShape_fillsItsBox_downToTheBottom() {
    val outline =
        WaterColumnShape(amplitude = 3.dp, wavelength = 16.dp)
            .createOutline(Size(40f, 100f), LayoutDirection.Ltr, Density(1f))

    val bounds = (outline as Outline.Generic).path.getBounds()
    assertEquals(0f, bounds.left, 0f)
    assertEquals(100f, bounds.bottom, 0f)
    assertTrue(bounds.top >= 0f && bounds.top <= 6f)
  }

  @Test
  fun rippleRadius_growsWithProgress_upToTheRingSize() {
    assertEquals(40f, rippleRadius(maxRadius = 100f, fraction = 1f, progress = 0f), 0.01f)
    assertEquals(100f, rippleRadius(maxRadius = 100f, fraction = 1f, progress = 1f), 0.01f)
    assertEquals(35f, rippleRadius(maxRadius = 100f, fraction = 0.35f, progress = 1f), 0.01f)
  }

  @Test
  fun rippleRadius_startsTheOuterRingsLater() {
    val inner = rippleRadius(100f, 0.35f, 0.3f) / 35f
    val outer = rippleRadius(100f, 1f, 0.3f) / 100f
    assertTrue(inner > outer)
  }
}
