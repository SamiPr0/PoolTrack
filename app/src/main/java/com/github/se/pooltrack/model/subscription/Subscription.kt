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
 */
@Serializable
data class Subscription(
    val id: String,
    val uri: String,
    val displayName: String,
    val addedAtEpochMilli: Long,
)

/** The instant this subscription was added. */
val Subscription.addedAt: Instant
  get() = Instant.ofEpochMilli(addedAtEpochMilli)
