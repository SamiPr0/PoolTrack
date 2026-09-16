package com.github.se.pooltrack.model.subscription

import kotlinx.coroutines.flow.Flow

/** Represents a repository that stores the URI of the user's subscription PDF. */
interface SubscriptionRepository {

  /** Emits the currently stored subscription URI, or `null` if none was picked yet. */
  fun getSubscriptionUri(): Flow<String?>

  /**
   * Stores the URI of the subscription PDF the user picked.
   *
   * @param uri The content URI of the PDF, as a string.
   */
  suspend fun setSubscriptionUri(uri: String)
}
