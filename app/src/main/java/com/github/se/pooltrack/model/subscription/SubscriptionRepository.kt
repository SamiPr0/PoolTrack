package com.github.se.pooltrack.model.subscription

import java.time.Instant
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

  /** Emits when the subscription pass was last shown to the user, or `null` if never. */
  fun getLastOpenedAt(): Flow<Instant?>

  /**
   * Records that the subscription pass was just shown to the user.
   *
   * @param at The instant it was opened.
   */
  suspend fun setLastOpenedAt(at: Instant)
}
