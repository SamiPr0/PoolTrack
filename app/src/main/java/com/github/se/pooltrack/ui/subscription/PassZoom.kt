package com.github.se.pooltrack.ui.subscription

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MIN_ZOOM_SCALE = 1f
const val MAX_ZOOM_SCALE = 6f

/**
 * How the pass is currently zoomed and panned. [offset] is the translation, in pixels, of the
 * content's center from the viewport's center, so [IDENTITY] is the un-zoomed pass.
 */
data class PassTransform(val scale: Float = MIN_ZOOM_SCALE, val offset: Offset = Offset.Zero) {
  companion object {
    val IDENTITY = PassTransform()
  }
}

/**
 * Applies one gesture step to this transform: zooms by [zoomChange] around [centroid] (relative to
 * the viewport's center, so the point under the fingers stays put) and then pans by [pan]. The
 * scale is kept within [MIN_ZOOM_SCALE]..[MAX_ZOOM_SCALE] and the offset is clamped so the content
 * never leaves a gap at the viewport's edges.
 */
fun PassTransform.transformedBy(
    zoomChange: Float,
    centroid: Offset,
    pan: Offset,
    viewport: Size,
): PassTransform {
  val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
  val ratio = newScale / scale
  val zoomedOffset = centroid - (centroid - offset) * ratio
  return PassTransform(newScale, (zoomedOffset + pan).clampedFor(newScale, viewport))
}

private fun Offset.clampedFor(scale: Float, viewport: Size): Offset {
  val maxX = (scale - 1f) * viewport.width / 2f
  val maxY = (scale - 1f) * viewport.height / 2f
  return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}

/**
 * Holds the pass's zoom/pan. Scoped to the activity (not the navigation entry), so it survives
 * leaving the quick view - after an accepted scan or going back home - and coming back to it.
 */
class PassZoomViewModel : ViewModel() {
  private val _transform = MutableStateFlow(PassTransform.IDENTITY)
  val transform: StateFlow<PassTransform> = _transform.asStateFlow()

  private var forUri: String? = null

  /** Resets the zoom when a different subscription's pass is shown than the one last zoomed. */
  fun onPassShown(uri: String) {
    if (forUri != uri) {
      forUri = uri
      _transform.value = PassTransform.IDENTITY
    }
  }

  /** Applies a pinch/pan gesture step; see [transformedBy]. */
  fun onGesture(zoomChange: Float, centroid: Offset, pan: Offset, viewport: Size) {
    _transform.value = _transform.value.transformedBy(zoomChange, centroid, pan, viewport)
  }
}
