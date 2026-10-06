package com.github.se.pooltrack.ui.subscription

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class PassZoomTest {
  private val viewport = Size(1000f, 2000f)

  @Test
  fun zoomAroundCenter_keepsOffsetZero() {
    val t = PassTransform.IDENTITY.transformedBy(2f, Offset.Zero, Offset.Zero, viewport)
    assertEquals(2f, t.scale, 0f)
    assertEquals(Offset.Zero, t.offset)
  }

  @Test
  fun zoomAwayFromCenter_keepsPointUnderFingersInPlace() {
    val centroid = Offset(400f, -800f)
    val t = PassTransform.IDENTITY.transformedBy(2f, centroid, Offset.Zero, viewport)
    assertEquals(Offset(-400f, 800f), t.offset)
  }

  @Test
  fun scale_isClampedToBounds() {
    val big = PassTransform.IDENTITY.transformedBy(100f, Offset.Zero, Offset.Zero, viewport)
    assertEquals(MAX_ZOOM_SCALE, big.scale, 0f)
    val small = big.transformedBy(0.001f, Offset.Zero, Offset.Zero, viewport)
    assertEquals(MIN_ZOOM_SCALE, small.scale, 0f)
    assertEquals(Offset.Zero, small.offset)
  }

  @Test
  fun pan_isClampedToContentEdges() {
    val t =
        PassTransform(2f, Offset.Zero)
            .transformedBy(1f, Offset.Zero, Offset(9999f, -9999f), viewport)
    assertEquals(Offset(500f, -1000f), t.offset)
  }

  @Test
  fun pan_atMinScale_isIgnored() {
    val t = PassTransform.IDENTITY.transformedBy(1f, Offset.Zero, Offset(50f, 50f), viewport)
    assertEquals(Offset.Zero, t.offset)
  }

  @Test
  fun viewModel_keepsTransform_untilDifferentPassShown() {
    val vm = PassZoomViewModel()
    vm.onPassShown("a")
    vm.onGesture(3f, Offset(100f, 100f), Offset.Zero, viewport)
    val zoomed = vm.transform.value
    vm.onPassShown("a")
    assertEquals(zoomed, vm.transform.value)
    vm.onPassShown("b")
    assertEquals(PassTransform.IDENTITY, vm.transform.value)
  }
}
