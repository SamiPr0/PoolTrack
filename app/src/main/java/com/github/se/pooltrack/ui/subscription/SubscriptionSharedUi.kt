package com.github.se.pooltrack.ui.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.se.pooltrack.model.subscription.remainingSubscriptionCooldown
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Cooldown logic shared by [com.github.se.pooltrack.ui.home.HomeScreen] (which grays out its FAB
 * during the cooldown) and [SubscriptionQuickViewScreen] (which shows a locked state instead of the
 * pass) - both need to know the same "can the pass be shown right now" answer.
 */

/** How often the cooldown countdown re-checks the current time while a screen is on top. */
private val COOLDOWN_REFRESH_INTERVAL = Duration.ofSeconds(30)

/** Tracks [lastEntryTimestamp] against a periodically-refreshed "now", recomposing as it ticks. */
@Composable
fun rememberRemainingCooldown(lastEntryTimestamp: Instant?): Duration? {
  var now by remember { mutableStateOf(Instant.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(COOLDOWN_REFRESH_INTERVAL.toMillis())
      now = Instant.now()
    }
  }
  return remainingSubscriptionCooldown(lastEntryTimestamp, now)
}

fun formatCooldown(remaining: Duration): String {
  val hours = remaining.toHours()
  val minutes = remaining.minusHours(hours).toMinutes()
  return when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
    hours > 0 -> "${hours}h"
    minutes > 0 -> "${minutes}m"
    else -> "less than a minute"
  }
}
