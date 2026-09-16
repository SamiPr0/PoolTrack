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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.renderFirstPdfPage
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SubscriptionQuickViewScreenTestTags {
  const val LOADING_INDICATOR = "SubscriptionQuickViewScreenLoadingIndicator"
  const val PDF_IMAGE = "SubscriptionQuickViewScreenPdfImage"
  const val ACCEPT_BUTTON = "SubscriptionQuickViewScreenAcceptButton"
  const val COOLDOWN_MESSAGE = "SubscriptionQuickViewScreenCooldownMessage"
}

/**
 * The focused flow opened from Home's FAB: shows the active pass, an Accept button, and a way
 * back - nothing else. Managing subscriptions (adding, switching, deleting) is reserved for the
 * [SubscriptionScreen] tab, reached from the bottom bar.
 */
@Composable
fun SubscriptionQuickViewScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val activeSubscription by viewModel.activeSubscription.collectAsState()
  val lastEntryTimestamp by viewModel.lastEntryTimestamp.collectAsState()
  val remainingCooldown = rememberRemainingCooldown(lastEntryTimestamp)

  if (remainingCooldown == null && activeSubscription != null) {
    MaxBrightness()
  }

  Scaffold(
      topBar = {
        TopNavigationMenu(
            Screen.SubscriptionQuickView,
            onGoBack = { navigationActions?.goBack() },
        )
      },
  ) { paddingValues ->
    val cooldown = remainingCooldown
    val currentUri = activeSubscription?.uri
    if (cooldown != null) {
      CooldownLockedMessage(
          cooldown = cooldown,
          modifier = Modifier.padding(paddingValues),
      )
    } else if (currentUri == null) {
      Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No active subscription",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Text(
            text = "Add or activate one from the Subscriptions tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      PassDisplayAndAccept(
          uri = currentUri,
          onAccepted = {
            viewModel.onScannerAccepted()
            navigationActions?.goBack()
          },
          modifier = Modifier.padding(paddingValues),
      )
    }
  }
}

/** Shown instead of the pass while its reopen cooldown is active. */
@Composable
private fun CooldownLockedMessage(cooldown: Duration, modifier: Modifier = Modifier) {
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
        modifier = Modifier.testTag(SubscriptionQuickViewScreenTestTags.COOLDOWN_MESSAGE),
    )
  }
}

/** Renders the pass full-screen plus the Accept button; calls [onAccepted] once tapped. */
@Composable
private fun PassDisplayAndAccept(
    uri: String,
    onAccepted: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier.testTag(SubscriptionQuickViewScreenTestTags.LOADING_INDICATOR),
        )
      } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Subscription pass",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier.fillMaxSize().testTag(SubscriptionQuickViewScreenTestTags.PDF_IMAGE),
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
          modifier = Modifier.testTag(SubscriptionQuickViewScreenTestTags.ACCEPT_BUTTON),
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
private fun MaxBrightness() {
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
