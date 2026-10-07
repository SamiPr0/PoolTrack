package com.github.se.pooltrack.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.entry.timestamp
import com.github.se.pooltrack.model.swim.formatSwimDuration
import com.github.se.pooltrack.model.swim.isSwimPending
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object EntryDetailsScreenTestTags {
  const val NOT_FOUND_MESSAGE = "EntryDetailsScreenNotFoundMessage"
  const val DATE = "EntryDetailsScreenDate"
  const val SWIM_DURATION = "EntryDetailsScreenSwimDuration"
  const val SWIM_NUMBER = "EntryDetailsScreenSwimNumber"
  const val SUBSCRIPTION = "EntryDetailsScreenSubscription"
  const val COST = "EntryDetailsScreenCost"
  const val SINCE_PREVIOUS = "EntryDetailsScreenSincePrevious"
  const val DELETE_BUTTON = "EntryDetailsScreenDeleteButton"
  const val CONFIRM_DELETE_BUTTON = "EntryDetailsScreenConfirmDeleteButton"
  const val CANCEL_DELETE_BUTTON = "EntryDetailsScreenCancelDeleteButton"
}

private val ZONE = ZoneId.systemDefault()

/** EntryDetailsScreen shows everything known about one confirmed entry, opened from History. */
@Composable
fun EntryDetailsScreen(
    timestampEpochMilli: Long,
    viewModel: EntryDetailsViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  LaunchedEffect(timestampEpochMilli) { viewModel.loadEntry(timestampEpochMilli) }
  val details by viewModel.details.collectAsState()
  var confirmingDelete by remember { mutableStateOf(false) }

  if (confirmingDelete) {
    AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text("Delete this swim?") },
        text = { Text("It will be permanently removed from your history.") },
        confirmButton = {
          TextButton(
              onClick = {
                confirmingDelete = false
                viewModel.onDeleteEntry()
                navigationActions?.goBack()
              },
              modifier = Modifier.testTag(EntryDetailsScreenTestTags.CONFIRM_DELETE_BUTTON),
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(
              onClick = { confirmingDelete = false },
              modifier = Modifier.testTag(EntryDetailsScreenTestTags.CANCEL_DELETE_BUTTON),
          ) {
            Text("Cancel")
          }
        },
    )
  }

  Scaffold(
      topBar = {
        TopNavigationMenu(
            Screen.EntryDetails,
            onGoBack = { navigationActions?.goBack() },
            actions = {
              if (details != null) {
                IconButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.testTag(EntryDetailsScreenTestTags.DELETE_BUTTON),
                ) {
                  Icon(Icons.Outlined.Delete, contentDescription = "Delete swim")
                }
              }
            },
        )
      },
  ) { paddingValues ->
    val current = details
    if (current == null) {
      Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            text = "This swim no longer exists",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(EntryDetailsScreenTestTags.NOT_FOUND_MESSAGE),
        )
      }
    } else {
      Column(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .verticalScroll(rememberScrollState())
                  .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        SwimHero(current)
        DetailsCard(current)
      }
    }
  }
}

/** The date, entry time and swim duration, the answers to "when, and for how long?". */
@Composable
private fun SwimHero(details: EntryDetails) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
      Text(
          text = dateFormatter().format(details.entry.timestamp),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.testTag(EntryDetailsScreenTestTags.DATE),
      )
      Text(
          text = "Entered at ${timeFormatter().format(details.entry.timestamp)}",
          style = MaterialTheme.typography.bodyMedium,
      )
      Text(
          text = swimDurationLabel(details),
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
          modifier =
              Modifier.padding(top = 16.dp).testTag(EntryDetailsScreenTestTags.SWIM_DURATION),
      )
      Text(
          text = if (details.swimDuration != null) "in the water" else "swim duration",
          style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

private fun swimDurationLabel(details: EntryDetails): String =
    when {
      details.swimDuration != null -> formatSwimDuration(details.swimDuration)
      isSwimPending(details.entry) -> "In progress"
      else -> "Not recorded"
    }

@Composable
private fun DetailsCard(details: EntryDetails) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      DetailRow(
          label = "Swim",
          value = "#${details.swimNumber}",
          modifier = Modifier.testTag(EntryDetailsScreenTestTags.SWIM_NUMBER),
      )
      HorizontalDivider()
      DetailRow(
          label = "Subscription",
          value = subscriptionLabel(details),
          modifier = Modifier.testTag(EntryDetailsScreenTestTags.SUBSCRIPTION),
      )
      details.costOfEntry?.let { cost ->
        HorizontalDivider()
        DetailRow(
            label = "Cost of this entry",
            value = String.format(LocalLocale.current.platformLocale, "%.2f", cost),
            modifier = Modifier.testTag(EntryDetailsScreenTestTags.COST),
        )
      }
      HorizontalDivider()
      DetailRow(
          label = "Since previous swim",
          value = sincePreviousLabel(details.daysSincePreviousSwim),
          modifier = Modifier.testTag(EntryDetailsScreenTestTags.SINCE_PREVIOUS),
      )
    }
  }
}

/** "Pool 10x · entry 4 of 10", "Yearly pass · entry 4", "Deleted subscription" or "None". */
private fun subscriptionLabel(details: EntryDetails): String {
  val subscription =
      details.subscription
          ?: return if (details.subscriptionDeleted) "Deleted subscription" else "None"
  val number = details.entryNumberOnSubscription
  val position = subscription.maxEntries?.let { "entry $number of $it" } ?: "entry $number"
  return "${subscription.displayName} · $position"
}

private fun sincePreviousLabel(days: Long?): String =
    when (days) {
      null -> "First swim"
      0L -> "Same day"
      1L -> "1 day"
      else -> "$days days"
    }

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.End,
    )
  }
}

// Built fresh on every call so a locale change while the app runs is picked up (see History).
private fun dateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withLocale(Locale.getDefault())
        .withZone(ZONE)

private fun timeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZONE)
