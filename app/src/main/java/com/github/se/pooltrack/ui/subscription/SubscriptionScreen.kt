package com.github.se.pooltrack.ui.subscription

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.addedAt
import com.github.se.pooltrack.model.subscription.renderFirstPdfPage
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SubscriptionScreenTestTags {
  const val EMPTY_MESSAGE = "SubscriptionScreenEmptyMessage"
  const val EMPTY_ADD_BUTTON = "SubscriptionScreenEmptyAddButton"
  const val SUBSCRIPTION_LIST = "SubscriptionScreenList"
  const val SUBSCRIPTION_ITEM = "SubscriptionScreenItem"
  const val THUMBNAIL = "SubscriptionScreenThumbnail"
  const val ACTIVE_BADGE = "SubscriptionScreenActiveBadge"
  const val SET_ACTIVE_BUTTON = "SubscriptionScreenSetActiveButton"
  const val DELETE_BUTTON = "SubscriptionScreenDeleteButton"
  const val CONFIRM_DELETE_BUTTON = "SubscriptionScreenConfirmDeleteButton"
  const val CANCEL_DELETE_BUTTON = "SubscriptionScreenCancelDeleteButton"
  const val ADD_BUTTON = "SubscriptionScreenAddButton"
}

// Built fresh on every call rather than cached as a val, so a locale change while the app is
// running (without a process restart) is picked up instead of baked in at class-init time.
private fun addedDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

/**
 * SubscriptionScreen is the always-reachable bottom-nav tab for managing subscriptions: lists
 * every one the user has added - e.g. an expired one kept for reference alongside the new one
 * replacing it - lets them pick which is active, delete any of them, or add another.
 *
 * It deliberately does not display the active subscription's pass itself: that would just
 * duplicate [SubscriptionQuickViewScreen], which Home's FAB already opens for that.
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val subscriptions by viewModel.subscriptions.collectAsState()
  val activeSubscription by viewModel.activeSubscription.collectAsState()
  var subscriptionPendingDeletion by remember { mutableStateOf<Subscription?>(null) }

  val pickPdfLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onSubscriptionPicked(uri)
      }

  subscriptionPendingDeletion?.let { subscription ->
    AlertDialog(
        onDismissRequest = { subscriptionPendingDeletion = null },
        title = { Text("Delete this subscription?") },
        text = { Text("\"${subscription.displayName}\" will be permanently removed.") },
        confirmButton = {
          TextButton(
              onClick = {
                viewModel.onDeleteSubscription(subscription.id)
                subscriptionPendingDeletion = null
              },
              modifier = Modifier.testTag(SubscriptionScreenTestTags.CONFIRM_DELETE_BUTTON),
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(
              onClick = { subscriptionPendingDeletion = null },
              modifier = Modifier.testTag(SubscriptionScreenTestTags.CANCEL_DELETE_BUTTON),
          ) {
            Text("Cancel")
          }
        },
    )
  }

  Scaffold(
      topBar = {
        TopNavigationMenu(
            Screen.Subscription,
            actions = {
              IconButton(
                  onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                  modifier = Modifier.testTag(SubscriptionScreenTestTags.ADD_BUTTON),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Add subscription")
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
    if (subscriptions.isEmpty()) {
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
            text = "No subscriptions yet",
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier.padding(top = 16.dp, bottom = 4.dp)
                    .testTag(SubscriptionScreenTestTags.EMPTY_MESSAGE),
        )
        Text(
            text = "Subscriptions you add will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.testTag(SubscriptionScreenTestTags.EMPTY_ADD_BUTTON),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add subscription PDF")
        }
      }
    } else {
      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .testTag(SubscriptionScreenTestTags.SUBSCRIPTION_LIST),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(subscriptions, key = { it.id }) { subscription ->
          SubscriptionRow(
              subscription = subscription,
              isActive = subscription.id == activeSubscription?.id,
              onSetActive = { viewModel.onSetActive(subscription.id) },
              onDelete = { subscriptionPendingDeletion = subscription },
          )
        }
      }
    }
  }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
  // Two rows rather than one long one: cramming the thumbnail, title, date, "Set active"
  // button and delete icon onto a single line left no room for the date, which wrapped
  // awkwardly and made cards uneven heights. Info on top, actions below, has room to breathe.
  Card(
      modifier = Modifier.fillMaxWidth().testTag(SubscriptionScreenTestTags.SUBSCRIPTION_ITEM),
      colors =
          if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          } else {
            CardDefaults.cardColors()
          },
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isActive) {
          Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Active",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp).testTag(SubscriptionScreenTestTags.ACTIVE_BADGE),
          )
        } else {
          Spacer(modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))

        SubscriptionThumbnail(uri = subscription.uri)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = subscription.displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text = "Added ${addedDateFormatter().format(subscription.addedAt)}",
              style = MaterialTheme.typography.bodySmall,
              color =
                  if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (isActive) {
          Text(
              text = "Active",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
          )
        } else {
          FilledTonalButton(
              onClick = onSetActive,
              modifier = Modifier.testTag(SubscriptionScreenTestTags.SET_ACTIVE_BUTTON),
          ) {
            Text("Set active")
          }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(SubscriptionScreenTestTags.DELETE_BUTTON),
        ) {
          Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Delete subscription",
              tint =
                  if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

/** A small preview of the PDF's first page, rendered asynchronously. */
@Composable
private fun SubscriptionThumbnail(uri: String) {
  val context = LocalContext.current
  var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uri) {
    bitmap = withContext(Dispatchers.IO) { renderFirstPdfPage(context, Uri.parse(uri)) }
  }

  Box(
      modifier =
          Modifier.size(width = 48.dp, height = 64.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag(SubscriptionScreenTestTags.THUMBNAIL),
  ) {
    bitmap?.let {
      Image(
          bitmap = it.asImageBitmap(),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
