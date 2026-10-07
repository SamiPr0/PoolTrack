package com.github.se.pooltrack.model.subscription

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.passZoomDataStore by preferencesDataStore(name = "pass_zoom_prefs")

private val PASS_URI_KEY = stringPreferencesKey("pass_uri")
private val SCALE_KEY = floatPreferencesKey("scale")
private val OFFSET_X_KEY = floatPreferencesKey("offset_x_fraction")
private val OFFSET_Y_KEY = floatPreferencesKey("offset_y_fraction")

/**
 * Stores the pass zoom on-device, using Jetpack DataStore. Only the zoom of the pass shown last is
 * kept: it is a per-device viewing preference, so it is neither scoped to an account nor backed up,
 * and keeping one slot means nothing stale piles up for deleted subscriptions.
 */
class PassZoomRepositoryLocal(private val context: Context) : PassZoomRepository {

  override suspend fun load(uri: String): SavedPassZoom? {
    val prefs = context.passZoomDataStore.data.first()
    if (prefs[PASS_URI_KEY] != uri) return null
    val scale = prefs[SCALE_KEY] ?: return null
    val offsetX = prefs[OFFSET_X_KEY] ?: return null
    val offsetY = prefs[OFFSET_Y_KEY] ?: return null
    if (!scale.isFinite() || !offsetX.isFinite() || !offsetY.isFinite()) return null
    return SavedPassZoom(scale, offsetX, offsetY)
  }

  override suspend fun save(uri: String, zoom: SavedPassZoom) {
    context.passZoomDataStore.edit { prefs ->
      prefs[PASS_URI_KEY] = uri
      prefs[SCALE_KEY] = zoom.scale
      prefs[OFFSET_X_KEY] = zoom.offsetXFraction
      prefs[OFFSET_Y_KEY] = zoom.offsetYFraction
    }
  }
}
