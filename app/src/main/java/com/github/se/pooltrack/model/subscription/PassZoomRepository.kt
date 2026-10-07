package com.github.se.pooltrack.model.subscription

/**
 * How far the user zoomed in on a pass. The offset is stored as a fraction of the viewport (not
 * pixels), so the zoom still lands on the QR code if the viewport's size changes, e.g. on rotation.
 *
 * @property scale The zoom factor, 1 being the un-zoomed pass.
 * @property offsetXFraction The horizontal pan, as a fraction of the viewport's width.
 * @property offsetYFraction The vertical pan, as a fraction of the viewport's height.
 */
data class SavedPassZoom(
    val scale: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float,
)

/** Represents a repository that remembers how the user zoomed in on their pass. */
interface PassZoomRepository {

  /**
   * The zoom last saved for the pass at [uri], or `null` if none was saved for it (or what was
   * stored is unusable), in which case the pass shows un-zoomed.
   */
  suspend fun load(uri: String): SavedPassZoom?

  /** Remembers [zoom] for the pass at [uri], replacing whatever was saved before. */
  suspend fun save(uri: String, zoom: SavedPassZoom)
}
