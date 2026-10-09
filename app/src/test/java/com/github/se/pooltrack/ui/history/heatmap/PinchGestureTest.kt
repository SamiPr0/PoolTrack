package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinchGestureTest {

  @Test
  fun zoomChangeFor_isOut_whenThePinchShrinksEnough() {
    assertEquals(ZoomChange.Out, zoomChangeFor(0.8f))
    assertEquals(ZoomChange.Out, zoomChangeFor(0.5f))
  }

  @Test
  fun zoomChangeFor_isIn_whenThePinchGrowsEnough() {
    assertEquals(ZoomChange.In, zoomChangeFor(1.25f))
    assertEquals(ZoomChange.In, zoomChangeFor(2f))
  }

  @Test
  fun zoomChangeFor_isNull_forSmallMovements() {
    assertNull(zoomChangeFor(1f))
    assertNull(zoomChangeFor(0.9f))
    assertNull(zoomChangeFor(1.2f))
  }
}
