package com.github.se.pooltrack.ui.subscription

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.model.subscription.remainingSubscriptionCooldown
import com.github.se.pooltrack.model.subscription.renderFirstPdfPage
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * UI shared between [SubscriptionScreen] (the always-reachable bottom-nav tab, which can also
 * manage subscriptions) and [SubscriptionQuickViewScreen] (the focused "just show my pass" flow
 * opened from Home's FAB) - both display the same pass under the same cooldown rule, they just
 * wrap it in a different Scaffold.
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

/** Shown instead of the pass while its reopen cooldown is active. */
@Composable
fun CooldownLockedMessage(cooldown: Duration, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "Already entered the pool",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
    Text(
        text = "You can show your pass again in ${formatCooldown(cooldown)}.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(SubscriptionScreenTestTags.COOLDOWN_MESSAGE),
    )
  }
}

/** Renders the pass full-screen plus the Accept button; calls [onAccepted] once tapped. */
@Composable
fun PassDisplayAndAccept(uri: String, onAccepted: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var pageBitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uri) {
    pageBitmap = withContext(Dispatchers.IO) { renderFirstPdfPage(context, Uri.parse(uri)) }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
      val bitmap = pageBitmap
      if (bitmap == null) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(SubscriptionScreenTestTags.LOADING_INDICATOR),
        )
      } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Subscription pass",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().testTag(SubscriptionScreenTestTags.PDF_IMAGE),
        )
      }
    }

    // A scan the scanner declines must not count as an entry. There's no dedicated
    // "Declined" control for that: simply not tapping Accepted already leaves nothing
    // recorded, and the pass stays up for the user to try scanning again.
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
      Button(
          onClick = onAccepted,
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          modifier = Modifier.testTag(SubscriptionScreenTestTags.ACCEPT_BUTTON),
      ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Accepted")
      }
    }
  }
}

/** Forces the current activity's screen brightness to maximum while this composable is shown. */
@Composable
fun MaxBrightness() {
  val activity = LocalContext.current as? Activity ?: return
  DisposableEffect(activity) {
    val window = activity.window
    val originalBrightness = window.attributes.screenBrightness

    val maxParams = window.attributes
    maxParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    window.attributes = maxParams

    onDispose {
      val restoredParams = window.attributes
      restoredParams.screenBrightness = originalBrightness
      window.attributes = restoredParams
    }
  }
}
