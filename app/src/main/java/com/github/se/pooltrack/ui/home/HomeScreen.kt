package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.daysUntilExpiration
import com.github.se.pooltrack.model.subscription.isExpired
import com.github.se.pooltrack.model.subscription.pricePerEntry
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import com.github.se.pooltrack.ui.subscription.formatCooldown
import com.github.se.pooltrack.ui.subscription.rememberRemainingCooldown
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

object HomeScreenTestTags {
  const val LAST_SWIM_HERO = "HomeScreenLastSwimHero"
  const val THIS_WEEK_STAT = "HomeScreenThisWeekStat"
  const val THIS_MONTH_STAT = "HomeScreenThisMonthStat"
  const val TOTAL_ENTRIES_STAT = "HomeScreenTotalEntriesStat"
  const val AVERAGE_PER_WEEK_STAT = "HomeScreenAveragePerWeekStat"
  const val FAVORITE_DAY_BANNER = "HomeScreenFavoriteDayBanner"
  const val EXPIRATION_BANNER = "HomeScreenExpirationBanner"
  const val NO_SUBSCRIPTION_TODO = "HomeScreenNoSubscriptionTodo"
  const val TOTAL_SPENT_STAT = "HomeScreenTotalSpentStat"
  const val COST_PER_ENTRY_STAT = "HomeScreenCostPerEntryStat"
  const val OPEN_SUBSCRIPTION_FAB = "HomeScreenOpenSubscriptionFab"
}

/** HomeScreen: the app's landing page, showing pool-visit stats. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val stats by viewModel.stats.collectAsState()
  val lastEntryTimestamp by viewModel.lastEntryTimestamp.collectAsState()
  val activeSubscription by viewModel.activeSubscription.collectAsState()
  val totalSpent by viewModel.totalSpent.collectAsState()
  val remainingCooldown = rememberRemainingCooldown(lastEntryTimestamp)

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
      topBar = { TopNavigationMenu(Screen.Home) },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Home,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      floatingActionButton = {
        // Stays clickable even while "locked" (rather than enabled = false) so tapping it can
        // explain why, instead of silently doing nothing.
        val cooldown = remainingCooldown
        FloatingActionButton(
            onClick = {
              if (cooldown != null) {
                coroutineScope.launch {
                  snackbarHostState.showSnackbar(
                      "Already entered the pool — reopen in ${formatCooldown(cooldown)}"
                  )
                }
              } else {
                navigationActions?.navigateTo(Screen.SubscriptionQuickView)
              }
            },
            containerColor =
                if (cooldown != null) MaterialTheme.colorScheme.surfaceVariant
                else FloatingActionButtonDefaults.containerColor,
            contentColor =
                if (cooldown != null) MaterialTheme.colorScheme.onSurfaceVariant
                else contentColorFor(FloatingActionButtonDefaults.containerColor),
            modifier = Modifier.testTag(HomeScreenTestTags.OPEN_SUBSCRIPTION_FAB),
        ) {
          Icon(Icons.Filled.Person, contentDescription = "Open subscription")
        }
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

      val subscription = activeSubscription
      if (subscription != null) {
        ExpirationBanner(
            subscription = subscription,
            modifier = Modifier.testTag(HomeScreenTestTags.EXPIRATION_BANNER),
        )
      } else {
        NoSubscriptionTodoCard(
            onClick = { navigationActions?.navigateTo(Screen.Subscription) },
            modifier = Modifier.testTag(HomeScreenTestTags.NO_SUBSCRIPTION_TODO),
        )
      }

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
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            icon = Icons.Outlined.Info,
            label = "Total spent",
            value = amountLabel(totalSpent),
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_SPENT_STAT),
        )
        StatTile(
            icon = Icons.Outlined.Info,
            label = "Cost / entry",
            value = amountLabel(activeSubscription?.pricePerEntry),
            modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.COST_PER_ENTRY_STAT),
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

/** Draws attention to the active subscription's expiration, in red once it's urgent or past. */
@Composable
private fun ExpirationBanner(subscription: Subscription, modifier: Modifier = Modifier) {
  val days = subscription.daysUntilExpiration()
  val message =
      when {
        days != null && days < 0 ->
            "Your active pass expired ${-days} ${if (-days == 1L) "day" else "days"} ago"
        days == 0L -> "Your active pass expires today"
        days == 1L -> "Your active pass expires tomorrow"
        days != null -> "Your active pass expires in $days days"
        subscription.maxEntries != null ->
            "Your active pass is limited to ${subscription.maxEntries} entries"
        else -> "Your active pass has no expiration"
      }
  val isUrgent = subscription.isExpired || (days != null && days in 0..7)
  val containerColor =
      if (isUrgent) MaterialTheme.colorScheme.errorContainer
      else MaterialTheme.colorScheme.surfaceVariant
  val contentColor =
      if (isUrgent) MaterialTheme.colorScheme.onErrorContainer
      else MaterialTheme.colorScheme.onSurfaceVariant

  Card(
      modifier = modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
          imageVector = if (isUrgent) Icons.Filled.Lock else Icons.Outlined.Info,
          contentDescription = null,
          tint = contentColor,
      )
      Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}

/** A to-do-style prompt shown on Home whenever there's no active subscription to track with. */
@Composable
private fun NoSubscriptionTodoCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = null)
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
            text = "To do: add a subscription",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Add or activate one to start tracking your entries.",
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

/** "29.90", or "-" if [amount] is `null` or zero (i.e. nothing meaningful was recorded). */
private fun amountLabel(amount: Double?): String =
    if (amount == null || amount == 0.0) "-" else String.format(Locale.getDefault(), "%.2f", amount)

private fun averagePerWeekLabel(averageEntriesPerWeek: Double?): String =
    if (averageEntriesPerWeek == null) "-"
    else String.format(Locale.getDefault(), "%.1f", averageEntriesPerWeek)

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
