package com.github.se.pooltrack.utils

import com.github.se.pooltrack.model.subscription.PassZoomRepository
import com.github.se.pooltrack.model.subscription.SavedPassZoom
import java.io.IOException

/**
 * [PassZoomRepository] for unit tests: keeps what was saved in memory, recording every save, and
 * throws [IOException] from [load] and [save] while [failing] is set.
 */
class FakePassZoomRepository(
    initial: Map<String, SavedPassZoom> = emptyMap(),
    var failing: Boolean = false,
) : PassZoomRepository {

  private val saved = initial.toMutableMap()

  /** Every `(uri, zoom)` passed to [save], in order. */
  val saves = mutableListOf<Pair<String, SavedPassZoom>>()

  override suspend fun load(uri: String): SavedPassZoom? {
    if (failing) throw IOException("load failed")
    return saved[uri]
  }

  override suspend fun save(uri: String, zoom: SavedPassZoom) {
    if (failing) throw IOException("save failed")
    saves += uri to zoom
    saved[uri] = zoom
  }
}
