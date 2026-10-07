package com.github.se.pooltrack.model.entry

import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a single confirmed pool entry, i.e. a scan the scanner accepted.
 *
 * @property timestampEpochMilli When the entry was confirmed, as epoch milliseconds (kept as a
 *   primitive since [kotlinx.serialization] has no built-in support for [Instant]).
 * @property subscriptionId The subscription that was active when this entry was confirmed, or
 *   `null` if none was (or for entries recorded before this was tracked). This is what lets an
 *   entry-limited subscription count how many of its entries have actually been used.
 * @property swimDurationMillis How long the swim lasted, in milliseconds, once the user picked
 *   their phone back up and it was reported to them; `null` while the swim is still in progress (or
 *   for entries recorded before this was tracked).
 */
@Serializable
data class Entry(
    val timestampEpochMilli: Long,
    val subscriptionId: String? = null,
    val swimDurationMillis: Long? = null,
)

/** The instant this entry was confirmed. */
val Entry.timestamp: Instant
  get() = Instant.ofEpochMilli(timestampEpochMilli)

/**
 * Whether [other] is the same entry, i.e. has the same timestamp and subscription. Unlike `==` this
 * ignores [Entry.swimDurationMillis], which changes after the entry was recorded, so an older
 * snapshot of an entry still matches it.
 */
fun Entry.isSameEntryAs(other: Entry): Boolean =
    timestampEpochMilli == other.timestampEpochMilli && subscriptionId == other.subscriptionId
