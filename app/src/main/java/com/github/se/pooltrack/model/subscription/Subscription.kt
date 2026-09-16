package com.github.se.pooltrack.model.subscription

import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * One subscription PDF the user has added. Several can exist at once (e.g. an expired one kept
 * for reference and the new one replacing it), but only one is ever active at a time.
 *
 * @property id A locally-generated unique identifier.
 * @property uri The content URI of the PDF, as a string.
 * @property displayName A human-readable label, e.g. the file's original name.
 * @property addedAtEpochMilli When this subscription was added, as epoch milliseconds (kept as a
 *   primitive since [kotlinx.serialization] has no built-in support for [Instant]).
 * @property expiresAtEpochMilli When this subscription expires, as epoch milliseconds, or `null`
 *   if it has no date-based expiration. Mutually exclusive with [maxEntries] in the add flow, but
 *   nothing prevents both being set.
 * @property maxEntries The number of entries this subscription is good for, or `null` if it has no
 *   entry-count limit.
 */
@Serializable
data class Subscription(
    val id: String,
    val uri: String,
    val displayName: String,
    val addedAtEpochMilli: Long,
    val expiresAtEpochMilli: Long? = null,
    val maxEntries: Int? = null,
)

/** The instant this subscription was added. */
val Subscription.addedAt: Instant
  get() = Instant.ofEpochMilli(addedAtEpochMilli)

/** The instant this subscription expires, or `null` if it has no date-based expiration. */
val Subscription.expiresAt: Instant?
  get() = expiresAtEpochMilli?.let { Instant.ofEpochMilli(it) }

/** Whether this subscription's expiration date, if any, has already passed. */
val Subscription.isExpired: Boolean
  get() = expiresAt?.isBefore(Instant.now()) == true
