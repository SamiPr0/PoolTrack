package com.github.se.pooltrack.ui.subscription

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu

object SubscriptionScreenTestTags {
  const val PICK_BUTTON = "SubscriptionScreenPickButton"
  const val CHOOSE_EXISTING_BUTTON = "SubscriptionScreenChooseExistingButton"
  const val LOADING_INDICATOR = "SubscriptionScreenLoadingIndicator"
  const val PDF_IMAGE = "SubscriptionScreenPdfImage"
  const val ACCEPT_BUTTON = "SubscriptionScreenAcceptButton"
  const val ADD_BUTTON = "SubscriptionScreenAddButton"
  const val MANAGE_BUTTON = "SubscriptionScreenManageButton"
  const val COOLDOWN_MESSAGE = "SubscriptionScreenCooldownMessage"
}

/**
 * SubscriptionScreen is the always-reachable bottom-nav tab: it displays the active subscription
 * PDF at maximum screen brightness, so it can be scanned at the pool entrance, and also lets the
 * user add or manage subscriptions. If none is active yet, it prompts the user to add one. If the
 * user entered the pool recently, it shows a cooldown message instead of the pass.
 *
 * For the focused "just show my pass" flow with no management actions, see
 * [SubscriptionQuickViewScreen].
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val activeSubscription by viewModel.activeSubscription.collectAsState()
  val subscriptions by viewModel.subscriptions.collectAsState()
  val lastEntryTimestamp by viewModel.lastEntryTimestamp.collectAsState()
  val remainingCooldown = rememberRemainingCooldown(lastEntryTimestamp)

  val pickPdfLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onSubscriptionPicked(uri)
      }

  // Only while the pass is actually being displayed - this is a persistent tab someone might
  // glance at without being at the scanner, unlike a screen you'd only deliberately open.
  if (remainingCooldown == null && activeSubscription != null) {
    MaxBrightness()
  }

  Scaffold(
      topBar = {
        TopNavigationMenu(
            Screen.Subscription,
            actions = {
              // Once a subscription is used up (e.g. a monthly pass expired), the user needs a
              // way to add a new one without losing track of the old one.
              if (activeSubscription != null) {
                IconButton(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.testTag(SubscriptionScreenTestTags.ADD_BUTTON),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Add subscription")
                }
              }
              if (subscriptions.isNotEmpty()) {
                IconButton(
                    onClick = { navigationActions?.navigateTo(Screen.SubscriptionList) },
                    modifier = Modifier.testTag(SubscriptionScreenTestTags.MANAGE_BUTTON),
                ) {
                  Icon(Icons.Filled.List, contentDescription = "Manage subscriptions")
                }
              }
            },
        )
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Subscription,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
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
            imageVector = Icons.Filled.Add,
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
            text = "Add your pool subscription PDF once, then show it at the scanner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.testTag(SubscriptionScreenTestTags.PICK_BUTTON),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add subscription PDF")
        }
        if (subscriptions.isNotEmpty()) {
          TextButton(
              onClick = { navigationActions?.navigateTo(Screen.SubscriptionList) },
              modifier =
                  Modifier.padding(top = 8.dp)
                      .testTag(SubscriptionScreenTestTags.CHOOSE_EXISTING_BUTTON),
          ) {
            Text("Or choose from your ${subscriptions.size} subscriptions")
          }
        }
      }
    } else {
      PassDisplayAndAccept(
          uri = currentUri,
          onAccepted = {
            viewModel.onScannerAccepted()
            navigationActions?.navigateTo(Screen.History)
          },
          modifier = Modifier.padding(paddingValues),
      )
    }
  }
}
