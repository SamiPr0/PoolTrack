package com.github.se.pooltrack.ui.entry

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.pricePerEntry
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Everything the Entry details screen shows about one confirmed entry.
 *
 * @property entry The entry itself.
 * @property swimDuration How long the swim lasted, or `null` if it was not recorded.
 * @property swimNumber The entry's 1-based position among every entry, oldest first.
 * @property subscription The subscription the entry counted against, or `null` if there was none or
 *   it has since been deleted.
 * @property entryNumberOnSubscription The entry's 1-based position among the entries of
 *   [subscription], oldest first, or `null` if there is no [subscription].
 * @property costOfEntry What this entry cost on an entry-limited [subscription], or `null` if that
 *   can't be known.
 * @property daysSincePreviousSwim Calendar days between the previous entry and this one, or `null`
 *   if this is the first entry.
 * @property subscriptionDeleted Whether the entry was tagged with a subscription that has since
 *   been deleted. `false` for an entry that never had one.
 */
data class EntryDetails(
    val entry: Entry,
    val swimDuration: Duration?,
    val swimNumber: Int,
    val subscription: Subscription?,
    val entryNumberOnSubscription: Int?,
    val costOfEntry: Double?,
    val daysSincePreviousSwim: Long?,
    val subscriptionDeleted: Boolean = false,
)

/**
 * Computes the [EntryDetails] of the entry confirmed at [timestampEpochMilli], or `null` if no such
 * entry is in [entries] (e.g. it was deleted). [zone] resolves calendar days.
 */
fun computeEntryDetails(
    timestampEpochMilli: Long,
    entries: List<Entry>,
    subscriptions: List<Subscription>,
    zone: ZoneId = ZoneId.systemDefault(),
): EntryDetails? {
  val oldestFirst = entries.sortedBy { it.timestampEpochMilli }
  val index = oldestFirst.indexOfFirst { it.timestampEpochMilli == timestampEpochMilli }
  if (index < 0) return null
  val entry = oldestFirst[index]

  val subscription = subscriptions.find { it.id == entry.subscriptionId }
  val entryNumberOnSubscription = subscription?.let { sub ->
    oldestFirst.take(index + 1).count { it.subscriptionId == sub.id }
  }
  val daysSincePreviousSwim =
      oldestFirst.getOrNull(index - 1)?.let { previous ->
        ChronoUnit.DAYS.between(localDate(previous, zone), localDate(entry, zone))
      }

  return EntryDetails(
      entry = entry,
      swimDuration = entry.swimDurationMillis?.let { Duration.ofMillis(it) },
      swimNumber = index + 1,
      subscription = subscription,
      entryNumberOnSubscription = entryNumberOnSubscription,
      costOfEntry = subscription?.pricePerEntry,
      daysSincePreviousSwim = daysSincePreviousSwim,
      subscriptionDeleted = entry.subscriptionId != null && subscription == null,
  )
}

private fun localDate(entry: Entry, zone: ZoneId) =
    Instant.ofEpochMilli(entry.timestampEpochMilli).atZone(zone).toLocalDate()
