package com.github.se.pooltrack.model.hint

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.hintDataStore by preferencesDataStore(name = "hint_prefs")

private fun dismissedKey(hint: Hint) = booleanPreferencesKey("dismissed_${hint.name}")

/**
 * Stores which hints were dismissed on-device, using Jetpack DataStore. Like the pass zoom, it is a
 * per-device preference: neither scoped to an account nor backed up.
 */
class HintRepositoryLocal(private val context: Context) : HintRepository {

  override fun isDismissed(hint: Hint): Flow<Boolean> =
      context.hintDataStore.data.map { it[dismissedKey(hint)] ?: false }.distinctUntilChanged()

  override suspend fun dismiss(hint: Hint) {
    context.hintDataStore.edit { it[dismissedKey(hint)] = true }
  }
}
