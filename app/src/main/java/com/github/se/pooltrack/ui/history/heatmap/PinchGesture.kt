package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** A pinch that shrinks the content by this factor (or more) counts as zooming out. */
internal const val ZOOM_OUT_FACTOR = 0.8f

/** A pinch that grows the content by this factor (or more) counts as zooming in. */
internal const val ZOOM_IN_FACTOR = 1.25f

/** What the total scale of a pinch means: [ZoomChange.Out], [ZoomChange.In] or neither. */
internal enum class ZoomChange {
  Out,
  In,
}

/** The [ZoomChange] for a pinch that scaled the content by [scale], or `null` if too small. */
internal fun zoomChangeFor(scale: Float): ZoomChange? =
    when {
      scale <= ZOOM_OUT_FACTOR -> ZoomChange.Out
      scale >= ZOOM_IN_FACTOR -> ZoomChange.In
      else -> null
    }

/**
 * Calls [onZoomOut] or [onZoomIn] once per two-finger pinch. One-finger touches are left alone, so
 * swiping a pager or tapping a day inside keeps working.
 */
internal fun Modifier.pinchToZoom(onZoomOut: () -> Unit, onZoomIn: () -> Unit): Modifier =
    pointerInput(onZoomOut, onZoomIn) {
      awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var scale = 1f
        var handled = false
        do {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          if (event.changes.count { it.pressed } >= 2 && !handled) {
            scale *= event.calculateZoom()
            when (zoomChangeFor(scale)) {
              ZoomChange.Out -> onZoomOut()
              ZoomChange.In -> onZoomIn()
              null -> Unit
            }
            handled = zoomChangeFor(scale) != null
          }
          if (handled || event.changes.count { it.pressed } >= 2) {
            event.changes.forEach { it.consume() }
          }
        } while (event.changes.any { it.pressed })
      }
    }
