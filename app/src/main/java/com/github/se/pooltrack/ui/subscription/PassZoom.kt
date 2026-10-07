package com.github.se.pooltrack.ui.subscription

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.subscription.PassZoomRepository
import com.github.se.pooltrack.model.subscription.PassZoomRepositoryProvider
import com.github.se.pooltrack.model.subscription.SavedPassZoom
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 * leaving the quick view - after an accepted scan or going back home - and coming back to it. The
 * zoom is also saved to [repository] after each gesture, so it is restored after the app is closed
 * and reopened.
 *
 * @property repository The repository the zoom is saved to and restored from.
 */
class PassZoomViewModel(
    private val repository: PassZoomRepository = PassZoomRepositoryProvider.repository,
) : ViewModel() {
  private val _transform = MutableStateFlow(PassTransform.IDENTITY)
  val transform: StateFlow<PassTransform> = _transform.asStateFlow()

  private var forUri: String? = null
  private var viewport = Size.Zero

  // Loaded from the repository, waiting for the viewport's size to turn its fractions into pixels.
  private var restored: SavedPassZoom? = null
  private var gestured = false
  private var loadJob: Job? = null

  // The latest zoom not yet written, with the pass it belongs to.
  private var pending: Pair<String, SavedPassZoom>? = null
  private var saveJob: Job? = null

  /**
   * Shows the zoom of the pass at [uri]: the one saved for it, or none if a different pass was
   * zoomed last. Does nothing while [uri] is the pass already shown.
   */
  fun onPassShown(uri: String) {
    if (forUri == uri) return
    forUri = uri
    restored = null
    gestured = false
    _transform.value = PassTransform.IDENTITY
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val saved =
          try {
            repository.load(uri)
          } catch (_: IOException) {
            null // An unreadable saved zoom is not worth failing the pass over.
          }
      // If the user already zoomed, their gesture wins over the slow load.
      if (!gestured) {
        restored = saved
        applyRestored()
      }
    }
  }

  /** Records the size of the area the pass is shown in, which the saved zoom is relative to. */
  fun onViewportChanged(viewport: Size) {
    this.viewport = viewport
    applyRestored()
  }

  /** Applies a pinch/pan gesture step; see [transformedBy]. */
  fun onGesture(zoomChange: Float, centroid: Offset, pan: Offset, viewport: Size) {
    this.viewport = viewport
    gestured = true // The user took over, so a zoom still being loaded must not undo this.
    restored = null
    _transform.value = _transform.value.transformedBy(zoomChange, centroid, pan, viewport)
    scheduleSave()
  }

  /** Writes the latest zoom now instead of waiting out the delay, e.g. when leaving the screen. */
  fun persistNow() {
    saveJob?.cancel()
    writePending()
  }

  private fun applyRestored() {
    val saved = restored ?: return
    if (viewport.width <= 0f || viewport.height <= 0f) return
    restored = null
    val offset =
        Offset(saved.offsetXFraction * viewport.width, saved.offsetYFraction * viewport.height)
    // Clamping also corrects a saved zoom that doesn't fit this viewport.
    _transform.value =
        PassTransform(saved.scale, offset).transformedBy(1f, Offset.Zero, Offset.Zero, viewport)
  }

  // A gesture reports many steps a second: wait for it to settle rather than write for each.
  private fun scheduleSave() {
    val uri = forUri ?: return
    if (viewport.width <= 0f || viewport.height <= 0f) return
    val current = _transform.value
    pending =
        uri to
            SavedPassZoom(
                scale = current.scale,
                offsetXFraction = current.offset.x / viewport.width,
                offsetYFraction = current.offset.y / viewport.height,
            )
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
      delay(SAVE_DELAY_MS)
      writePending()
    }
  }

  private fun writePending() {
    val (uri, zoom) = pending ?: return
    pending = null
    viewModelScope.launch {
      try {
        repository.save(uri, zoom)
      } catch (_: IOException) {
        // Losing the saved zoom just means the pass shows un-zoomed next time.
      }
    }
  }

  companion object {
    /** How long after the last gesture step the zoom is saved. */
    const val SAVE_DELAY_MS = 300L
  }
}
