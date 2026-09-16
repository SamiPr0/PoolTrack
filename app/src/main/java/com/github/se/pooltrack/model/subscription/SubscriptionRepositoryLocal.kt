package com.github.se.pooltrack.model.subscription

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

private val SUBSCRIPTIONS_KEY = stringPreferencesKey("subscriptions_json")
private val ACTIVE_SUBSCRIPTION_ID_KEY = stringPreferencesKey("active_subscription_id")

/** Stores the user's subscriptions on-device, using Jetpack DataStore. */
class SubscriptionRepositoryLocal(private val context: Context) : SubscriptionRepository {

  private val subscriptionsFlow: Flow<List<Subscription>> =
      context.subscriptionDataStore.data.map { prefs ->
        prefs[SUBSCRIPTIONS_KEY]?.let { Json.decodeFromString<List<Subscription>>(it) }
            ?: emptyList()
      }

  private val activeIdFlow: Flow<String?> =
      context.subscriptionDataStore.data.map { it[ACTIVE_SUBSCRIPTION_ID_KEY] }

  override fun getSubscriptions(): Flow<List<Subscription>> =
      subscriptionsFlow.map { subscriptions ->
        subscriptions.sortedByDescending { it.addedAtEpochMilli }
      }

  override fun getActiveSubscription(): Flow<Subscription?> =
      combine(subscriptionsFlow, activeIdFlow) { subscriptions, activeId ->
        subscriptions.find { it.id == activeId }
      }

  override suspend fun addSubscription(
      uri: String,
      displayName: String,
      expiresAtEpochMilli: Long?,
      maxEntries: Int?,
      price: Double?,
  ): Subscription {
    val subscription =
        Subscription(
            id = UUID.randomUUID().toString(),
            uri = uri,
            displayName = displayName,
            addedAtEpochMilli = System.currentTimeMillis(),
            expiresAtEpochMilli = expiresAtEpochMilli,
            maxEntries = maxEntries,
            price = price,
        )
    context.subscriptionDataStore.edit { prefs ->
      val existing =
          prefs[SUBSCRIPTIONS_KEY]?.let { Json.decodeFromString<List<Subscription>>(it) }
              ?: emptyList()
      prefs[SUBSCRIPTIONS_KEY] = Json.encodeToString(existing + subscription)
      prefs[ACTIVE_SUBSCRIPTION_ID_KEY] = subscription.id
    }
    return subscription
  }

  override suspend fun setActiveSubscription(id: String) {
    context.subscriptionDataStore.edit { it[ACTIVE_SUBSCRIPTION_ID_KEY] = id }
  }

  override suspend fun deleteSubscription(id: String) {
    context.subscriptionDataStore.edit { prefs ->
      val existing =
          prefs[SUBSCRIPTIONS_KEY]?.let { Json.decodeFromString<List<Subscription>>(it) }
              ?: emptyList()
      prefs[SUBSCRIPTIONS_KEY] = Json.encodeToString(existing.filter { it.id != id })
      if (prefs[ACTIVE_SUBSCRIPTION_ID_KEY] == id) {
        prefs.remove(ACTIVE_SUBSCRIPTION_ID_KEY)
      }
    }
  }
}
