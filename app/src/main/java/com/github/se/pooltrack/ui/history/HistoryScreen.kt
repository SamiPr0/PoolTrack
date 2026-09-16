package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            text = "No entries yet",
            modifier = Modifier.testTag(HistoryScreenTestTags.EMPTY_MESSAGE),
        )
      }
    } else {
      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .testTag(HistoryScreenTestTags.ENTRY_LIST),
      ) {
        items(entries) { entry: Entry ->
          Text(
              text = ENTRY_DATE_FORMATTER.format(entry.timestamp),
              modifier = Modifier.padding(16.dp).testTag(HistoryScreenTestTags.ENTRY_ITEM),
          )
        }
      }
    }
  }
}
