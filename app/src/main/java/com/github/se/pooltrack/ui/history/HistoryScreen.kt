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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import com.github.se.pooltrack.model.swim.formatSwimDistance
import com.github.se.pooltrack.model.swim.formatSwimDuration
import com.github.se.pooltrack.ui.history.heatmap.HistoryOverview
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

object HistoryScreenTestTags {
  const val EMPTY_MESSAGE = "HistoryScreenEmptyMessage"
  const val ENTRY_LIST = "HistoryScreenEntryList"
  const val DAY_HEADER = "HistoryScreenDayHeader"
  const val ENTRY_ITEM = "HistoryScreenEntryItem"
  const val DELETE_BUTTON = "HistoryScreenDeleteButton"
  const val CONFIRM_DELETE_BUTTON = "HistoryScreenConfirmDeleteButton"
  const val CANCEL_DELETE_BUTTON = "HistoryScreenCancelDeleteButton"
  const val ADD_PAST_ENTRY_BUTTON = "HistoryScreenAddPastEntryButton"
  const val UNDO_ADD_ACTION = "Undo"
  const val LINK_BANNER = "HistoryScreenLinkBanner"
  const val LINK_BUTTON = "HistoryScreenLinkButton"
  const val NO_ENTRIES_THAT_DAY = "HistoryScreenNoEntriesThatDay"
}

private val ZONE = ZoneId.systemDefault()

// Built fresh on every call rather than cached as a val, so a locale change while the app is
// running (without a process restart) is picked up instead of baked in at class-init time.
private fun entryTimeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZONE)

private fun entryDateTimeFormatter(): DateTimeFormatter =
    DateTimeFormatterBuilder()
        .appendPattern("EEEE, ")
        .append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        .toFormatter(Locale.getDefault())
        .withZone(ZONE)

/** HistoryScreen lists every confirmed pool entry, grouped by day, most recent first. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val entries by viewModel.entries.collectAsState()
  var entryPendingDeletion by remember { mutableStateOf<Entry?>(null) }
  var showAddSheet by remember { mutableStateOf(false) }
  var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
  // Remembered across openings, so adding several entries from the same day takes one tap each.
  var lastAddedDate by remember { mutableStateOf(LocalDate.now(ZONE)) }
  var lastAddedTime by remember { mutableStateOf(LocalTime.of(12, 0)) }
  val addResult by viewModel.addPastEntryResult.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val linkSuggestions by viewModel.linkSuggestions.collectAsState()

  // On success the sheet closes and an Undo snackbar appears. The snackbar is launched in its own
  // scope: clearing the result below would otherwise cancel this effect, and the snackbar with it.
  LaunchedEffect(addResult) {
    val added = addResult as? AddPastEntryResult.Added ?: return@LaunchedEffect
    showAddSheet = false
    viewModel.onAddPastEntryResultHandled()
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      val outcome =
          snackbarHostState.showSnackbar(
              message = "Entry added",
              actionLabel = HistoryScreenTestTags.UNDO_ADD_ACTION,
              withDismissAction = true,
          )
      if (outcome == SnackbarResult.ActionPerformed) viewModel.onDeleteEntry(added.entry)
    }
  }

  if (showAddSheet) {
    AddPastEntrySheet(
        initialDate = lastAddedDate,
        initialTime = lastAddedTime,
        error = (addResult as? AddPastEntryResult.Refused)?.error,
        onInputChanged = viewModel::onAddPastEntryResultHandled,
        onAdd = { date, time ->
          lastAddedDate = date
          lastAddedTime = time
          viewModel.onAddPastEntry(date.atTime(time).atZone(ZONE).toInstant())
        },
        onDismiss = {
          showAddSheet = false
          viewModel.onAddPastEntryResultHandled()
        },
    )
  }

  entryPendingDeletion?.let { entry ->
    AlertDialog(
        onDismissRequest = { entryPendingDeletion = null },
        title = { Text("Delete this entry?") },
        text = {
          Text(
              "The entry from ${entryDateTimeFormatter().format(entry.timestamp)} will be " +
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
      snackbarHost = { SnackbarHost(snackbarHostState) },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add past entry") },
            modifier = Modifier.testTag(HistoryScreenTestTags.ADD_PAST_ENTRY_BUTTON),
        )
      },
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
            modifier = Modifier.padding(top = 16.dp).testTag(HistoryScreenTestTags.EMPTY_MESSAGE),
        )
        Text(
            text = "Confirmed pool visits will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
      }
    } else {
      val today = LocalDate.now(ZONE)
      val allEntriesByDay = entries.groupBy { it.timestamp.atZone(ZONE).toLocalDate() }
      val countsByDay = remember(allEntriesByDay) { allEntriesByDay.mapValues { it.value.size } }
      val entriesByDay =
          selectedDate?.let { day -> allEntriesByDay.filterKeys { it == day } } ?: allEntriesByDay

      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .testTag(HistoryScreenTestTags.ENTRY_LIST),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
      ) {
        item(key = "overview") {
          HistoryOverview(
              countsByDay = countsByDay,
              today = today,
              firstDayOfWeek = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek,
              selectedDate = selectedDate,
              onDayClick = { day -> selectedDate = if (day == selectedDate) null else day },
              onClearSelection = { selectedDate = null },
              selectedLabel = { dayLabel(it, today) },
              modifier = Modifier.padding(vertical = 4.dp),
          )
        }
        if (selectedDate != null && entriesByDay.isEmpty()) {
          item(key = "no-entries-that-day") {
            Text(
                text = "No entries on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(vertical = 16.dp)
                        .testTag(HistoryScreenTestTags.NO_ENTRIES_THAT_DAY),
            )
          }
        }
        if (linkSuggestions.isNotEmpty()) {
          item(key = "link-banner") {
            LinkBanner(
                count = linkSuggestions.size,
                onLink = {
                  val links = linkSuggestions
                  viewModel.onLinkEntries(links)
                  scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val outcome =
                        snackbarHostState.showSnackbar(
                            message =
                                if (links.size == 1) "1 entry linked"
                                else "${links.size} entries linked",
                            actionLabel = HistoryScreenTestTags.UNDO_ADD_ACTION,
                            withDismissAction = true,
                        )
                    if (outcome == SnackbarResult.ActionPerformed) viewModel.onUnlinkEntries(links)
                  }
                },
                modifier = Modifier.padding(vertical = 4.dp),
            )
          }
        }
        entriesByDay.forEach { (day, entriesForDay) ->
          item(key = day.toEpochDay()) {
            DayHeader(label = dayLabel(day, today), entryCount = entriesForDay.size)
          }
          items(entriesForDay, key = { it.timestamp.toEpochMilli() }) { entry ->
            EntryRow(
                entry = entry,
                onClick = { navigationActions?.navigateToEntryDetails(entry.timestampEpochMilli) },
                onDelete = { entryPendingDeletion = entry },
                modifier = Modifier.padding(vertical = 4.dp),
            )
          }
        }
      }
    }
  }
}

/** Offers to attach entries that belong to no subscription to the one that covers them. */
@Composable
private fun LinkBanner(count: Int, onLink: () -> Unit, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth().testTag(HistoryScreenTestTags.LINK_BANNER)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
          text =
              if (count == 1) "1 entry isn't linked to a subscription"
              else "$count entries aren't linked to a subscription",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f).padding(vertical = 12.dp),
      )
      TextButton(
          onClick = onLink,
          modifier = Modifier.testTag(HistoryScreenTestTags.LINK_BUTTON),
      ) {
        Text("Link")
      }
    }
  }
}

/** Section header grouping entries from the same calendar day. */
@Composable
private fun DayHeader(label: String, entryCount: Int) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag(HistoryScreenTestTags.DAY_HEADER),
      color = MaterialTheme.colorScheme.background,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
      )
      Text(
          text = if (entryCount == 1) "1 entry" else "$entryCount entries",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** "Today", "Yesterday", or a weekday/date, omitting the year unless [day] isn't this year. */
private fun dayLabel(day: LocalDate, today: LocalDate): String =
    when (day) {
      today -> "Today"
      today.minusDays(1) -> "Yesterday"
      else -> {
        val pattern = if (day.year == today.year) "EEEE, MMM d" else "EEEE, MMM d, yyyy"
        day.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
      }
    }

/**
 * The entry time, followed by the swim duration (old entries) and the logged distance, whichever
 * were recorded: "18:42 · 52min · 1200 m".
 */
private fun entryRowLabel(entry: Entry): String =
    listOfNotNull(
            entryTimeFormatter().format(entry.timestamp),
            entry.swimDurationMillis?.let { formatSwimDuration(Duration.ofMillis(it)) },
            entry.swimDistanceMeters?.let { formatSwimDistance(it) },
        )
        .joinToString(" · ")

@Composable
private fun EntryRow(
    entry: Entry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Card(
      onClick = onClick,
      modifier = modifier.fillMaxWidth().testTag(HistoryScreenTestTags.ENTRY_ITEM),
  ) {
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
            text = entryRowLabel(entry),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
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
