package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object HistoryScreenTestTags {
  const val EMPTY_MESSAGE = "HistoryScreenEmptyMessage"
  const val ENTRY_LIST = "HistoryScreenEntryList"
  const val ENTRY_ITEM = "HistoryScreenEntryItem"
  const val DELETE_BUTTON = "HistoryScreenDeleteButton"
  const val CONFIRM_DELETE_BUTTON = "HistoryScreenConfirmDeleteButton"
  const val CANCEL_DELETE_BUTTON = "HistoryScreenCancelDeleteButton"
}

private val ENTRY_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

/** HistoryScreen lists every confirmed pool entry, most recent first. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val entries by viewModel.entries.collectAsState()
  var entryPendingDeletion by remember { mutableStateOf<Entry?>(null) }

  entryPendingDeletion?.let { entry ->
    AlertDialog(
        onDismissRequest = { entryPendingDeletion = null },
        title = { Text("Delete this entry?") },
        text = {
          Text(
              "The entry from ${ENTRY_DATE_FORMATTER.format(entry.timestamp)} will be " +
                  "permanently removed."
          )
        },
        confirmButton = {
          TextButton(
              onClick = {
                viewModel.onDeleteEntry(entry)
                entryPendingDeletion = null
              },
              modifier = Modifier.testTag(HistoryScreenTestTags.CONFIRM_DELETE_BUTTON),
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(
              onClick = { entryPendingDeletion = null },
              modifier = Modifier.testTag(HistoryScreenTestTags.CANCEL_DELETE_BUTTON),
          ) {
            Text("Cancel")
          }
        },
    )
  }

  Scaffold(
      topBar = { TopNavigationMenu(Screen.History) },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.History,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
        )
      },
  ) { paddingValues ->
    if (entries.isEmpty()) {
      Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
            imageVector = Icons.Outlined.DateRange,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No entries yet",
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier.padding(top = 16.dp).testTag(HistoryScreenTestTags.EMPTY_MESSAGE),
        )
        Text(
            text = "Confirmed pool visits will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
      }
    } else {
      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .testTag(HistoryScreenTestTags.ENTRY_LIST),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(entries) { entry: Entry ->
          EntryRow(entry, onDelete = { entryPendingDeletion = entry })
        }
      }
    }
  }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().testTag(HistoryScreenTestTags.ENTRY_ITEM)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = ENTRY_DATE_FORMATTER.format(entry.timestamp),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
        )
      }
      IconButton(
          onClick = onDelete,
          modifier = Modifier.testTag(HistoryScreenTestTags.DELETE_BUTTON),
      ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Delete entry",
            tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}
