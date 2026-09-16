package com.github.se.pooltrack.model.subscription

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

private val SUBSCRIPTION_URI_KEY = stringPreferencesKey("subscription_uri")
private val LAST_ENTERED_AT_KEY = stringPreferencesKey("last_entered_at")

/** Stores the subscription PDF URI on-device, using Jetpack DataStore. */
class SubscriptionRepositoryLocal(private val context: Context) : SubscriptionRepository {

  override fun getSubscriptionUri(): Flow<String?> =
      context.subscriptionDataStore.data.map { it[SUBSCRIPTION_URI_KEY] }

  override suspend fun setSubscriptionUri(uri: String) {
    context.subscriptionDataStore.edit { it[SUBSCRIPTION_URI_KEY] = uri }
  }

  override fun getLastEnteredAt(): Flow<Instant?> =
      context.subscriptionDataStore.data.map { prefs ->
        prefs[LAST_ENTERED_AT_KEY]?.let { Instant.parse(it) }
      }

  override suspend fun setLastEnteredAt(at: Instant) {
    context.subscriptionDataStore.edit { it[LAST_ENTERED_AT_KEY] = at.toString() }
  }
}
