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

  /** Emits when the user last entered the pool (a scanner-accepted scan), or `null` if never. */
  fun getLastEnteredAt(): Flow<Instant?>

  /**
   * Records that the user just entered the pool.
   *
   * @param at The instant of entry.
   */
  suspend fun setLastEnteredAt(at: Instant)
}
