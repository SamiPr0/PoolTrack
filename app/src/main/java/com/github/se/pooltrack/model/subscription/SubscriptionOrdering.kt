package com.github.se.pooltrack.model.subscription

/**
 * Orders subscriptions by expiration date, the farthest in the future first. Subscriptions without
 * a date-based expiration never expire, so they come first; already-expired ones end up last.
 * Subscriptions expiring at the same moment (or both without expiry) are ordered most recently
 * added first.
 */
fun List<Subscription>.sortedByExpirationDescending(): List<Subscription> =
    sortedWith(
        compareByDescending<Subscription> { it.expiresAtEpochMilli ?: Long.MAX_VALUE }
            .thenByDescending { it.addedAtEpochMilli }
    )
