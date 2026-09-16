package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu

object HomeScreenTestTags {
  const val THIS_WEEK_STAT = "HomeScreenThisWeekStat"
  const val LAST_SWIM_STAT = "HomeScreenLastSwimStat"
  const val TOTAL_ENTRIES_STAT = "HomeScreenTotalEntriesStat"
  const val OPEN_SUBSCRIPTION_BUTTON = "HomeScreenOpenSubscriptionButton"
}

/** HomeScreen: the app's landing page, showing pool-visit stats and a way to the subscription. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val stats by viewModel.stats.collectAsState()

  Scaffold(
      topBar = { TopNavigationMenu(Screen.Home) },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Home,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
        )
      },
  ) { paddingValues ->
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      StatCard(
          icon = Icons.Outlined.DateRange,
          label = "This week",
          value =
              if (stats.entriesThisWeek == 1) "1 time" else "${stats.entriesThisWeek} times",
          modifier = Modifier.testTag(HomeScreenTestTags.THIS_WEEK_STAT),
      )
      StatCard(
          icon = Icons.Outlined.Info,
          label = "Since last swim",
          value = lastSwimLabel(stats.daysSinceLastSwim),
          modifier = Modifier.testTag(HomeScreenTestTags.LAST_SWIM_STAT),
      )
      StatCard(
          icon = Icons.Filled.CheckCircle,
          label = "Total entries",
          value = "${stats.totalEntries}",
          modifier = Modifier.testTag(HomeScreenTestTags.TOTAL_ENTRIES_STAT),
      )

      Button(
          onClick = { navigationActions?.navigateTo(Screen.Subscription) },
          modifier =
              Modifier.fillMaxWidth()
                  .padding(top = 12.dp)
                  .testTag(HomeScreenTestTags.OPEN_SUBSCRIPTION_BUTTON),
      ) {
        Text("Open subscription")
      }
    }
  }
}

private fun lastSwimLabel(daysSinceLastSwim: Long?): String =
    when (daysSinceLastSwim) {
      null -> "No entries yet"
      0L -> "Today"
      1L -> "1 day"
      else -> "$daysSinceLastSwim days"
    }

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.padding(start = 16.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
      }
    }
  }
}
