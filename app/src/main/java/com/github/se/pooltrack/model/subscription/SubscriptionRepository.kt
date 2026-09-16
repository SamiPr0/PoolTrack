package com.github.se.pooltrack.model.subscription

import kotlinx.coroutines.flow.Flow

/** Represents a repository that manages the user's subscription PDFs. */
interface SubscriptionRepository {

  /** Emits every subscription that was ever added, most recently added first. */
  fun getSubscriptions(): Flow<List<Subscription>>

  /** Emits the currently active subscription, or `null` if none is active. */
  fun getActiveSubscription(): Flow<Subscription?>

  /**
   * Adds a new subscription and marks it active.
   *
   * @param uri The content URI of the PDF the user picked, as a string.
   * @param displayName A human-readable label for it.
   * @param expiresAtEpochMilli When it expires, as epoch milliseconds, or `null` for no date-based
   *   expiration.
   * @param maxEntries The number of entries it's good for, or `null` for no entry-count limit.
   * @return The newly created [Subscription].
   */
  suspend fun addSubscription(
      uri: String,
      displayName: String,
      expiresAtEpochMilli: Long? = null,
      maxEntries: Int? = null,
  ): Subscription

  /**
   * Marks the subscription with [id] as the active one, deactivating any other.
   *
   * @param id The identifier of the subscription to activate.
   */
  suspend fun setActiveSubscription(id: String)

  /**
   * Removes a subscription. If it was the active one, no subscription is active afterwards.
   *
   * @param id The identifier of the subscription to remove.
   */
  suspend fun deleteSubscription(id: String)
}
