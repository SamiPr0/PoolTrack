package com.github.se.pooltrack.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu

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
            text = "Add or activate one from the Subscription tab.",
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
