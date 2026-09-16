package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.remainingSubscriptionCooldown
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import java.time.Duration
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

object HomeScreenTestTags {
  const val LAST_SWIM_HERO = "HomeScreenLastSwimHero"
  const val THIS_WEEK_STAT = "HomeScreenThisWeekStat"
  const val THIS_MONTH_STAT = "HomeScreenThisMonthStat"
  const val TOTAL_ENTRIES_STAT = "HomeScreenTotalEntriesStat"
  const val AVERAGE_PER_WEEK_STAT = "HomeScreenAveragePerWeekStat"
  const val FAVORITE_DAY_BANNER = "HomeScreenFavoriteDayBanner"
  const val OPEN_SUBSCRIPTION_BUTTON = "HomeScreenOpenSubscriptionButton"
  const val COOLDOWN_MESSAGE = "HomeScreenCooldownMessage"
}

/** How often the cooldown countdown re-checks the current time while Home is on screen. */
private val COOLDOWN_REFRESH_INTERVAL = Duration.ofSeconds(30)

/** HomeScreen: the app's landing page, showing pool-visit stats and a way to the subscription. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val stats by viewModel.stats.collectAsState()
  val lastSubscriptionOpenAt by viewModel.lastSubscriptionOpenAt.collectAsState()

  var now by remember { mutableStateOf(Instant.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(COOLDOWN_REFRESH_INTERVAL.toMillis())
      now = Instant.now()
    }
  }
  val remainingCooldown = remainingSubscriptionCooldown(lastSubscriptionOpenAt, now)

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
        modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LastSwimHero(
          daysSinceLastSwim = stats.daysSinceLastSwim,
          modifier = Modifier.testTag(HomeScreenTestTags.LAST_SWIM_HERO),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            icon = Icons.Outlined.DateRange,
            label = "This week",
            value = "${stats.entriesThisWeek}",
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_WEEK_STAT),
        )
        StatTile(
            icon = Icons.Outlined.DateRange,
            label = "This month",
            value = "${stats.entriesThisMonth}",
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_MONTH_STAT),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            icon = Icons.Filled.CheckCircle,
            label = "Total entries",
            value = "${stats.totalEntries}",
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_ENTRIES_STAT),
        )
        StatTile(
            icon = Icons.Filled.Star,
            label = "Avg. per week",
            value = averagePerWeekLabel(stats.averageEntriesPerWeek),
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.AVERAGE_PER_WEEK_STAT),
        )
      }

      stats.favoriteDayOfWeek?.let { day ->
        val dayName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
        Card(
            modifier = Modifier.fillMaxWidth().testTag(HomeScreenTestTags.FAVORITE_DAY_BANNER),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "You usually swim on $dayName",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
      }

      Button(
          onClick = { navigationActions?.navigateTo(Screen.Subscription) },
          enabled = remainingCooldown == null,
          modifier =
              Modifier.fillMaxWidth()
                  .padding(top = 8.dp)
                  .testTag(HomeScreenTestTags.OPEN_SUBSCRIPTION_BUTTON),
          contentPadding = PaddingValues(vertical = 16.dp),
      ) {
        Text("Open subscription", style = MaterialTheme.typography.titleMedium)
      }
      remainingCooldown?.let { cooldown ->
        Text(
            text = "You can reopen it in ${formatCooldown(cooldown)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(HomeScreenTestTags.COOLDOWN_MESSAGE),
        )
      }
    }
  }
}

@Composable
private fun LastSwimHero(daysSinceLastSwim: Long?, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = lastSwimValueLabel(daysSinceLastSwim),
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
      )
      Text(
          text = "since your last swim",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

private fun lastSwimValueLabel(daysSinceLastSwim: Long?): String =
    when (daysSinceLastSwim) {
      null -> "No entries yet"
      0L -> "Today"
      1L -> "1 day"
      else -> "$daysSinceLastSwim days"
    }

private fun averagePerWeekLabel(averageEntriesPerWeek: Double?): String =
    if (averageEntriesPerWeek == null) "-"
    else String.format(Locale.getDefault(), "%.1f", averageEntriesPerWeek)

private fun formatCooldown(remaining: Duration): String {
  val hours = remaining.toHours()
  val minutes = remaining.minusHours(hours).toMinutes()
  return when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
    hours > 0 -> "${hours}h"
    minutes > 0 -> "${minutes}m"
    else -> "less than a minute"
  }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
      )
      Text(
          text = value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp),
      )
      Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
      )
    }
  }
}
