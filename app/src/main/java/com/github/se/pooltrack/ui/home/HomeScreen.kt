package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.ui.account.AccountButton
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import com.github.se.pooltrack.ui.subscription.formatCooldown
import com.github.se.pooltrack.ui.subscription.rememberRemainingCooldown
import com.github.se.pooltrack.ui.theme.DepthGauge
import com.github.se.pooltrack.ui.theme.LaneRope
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.PoolScaffold
import com.github.se.pooltrack.ui.theme.deepWater
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
  const val STREAK_STAT = "HomeScreenStreakStat"
  const val BEST_WEEK_STAT = "HomeScreenBestWeekStat"
  const val WEEKLY_CHART = "HomeScreenWeeklyChart"
  const val WEEKLY_CHART_LEGEND = "HomeScreenWeeklyChartLegend"
  const val WEEKLY_CHART_BAR = "HomeScreenWeeklyChartBar"
  const val OPEN_SUBSCRIPTION_FAB = "HomeScreenOpenSubscriptionFab"
  const val DEPTH_GAUGE = "HomeScreenDepthGauge"
  const val LANE_ROPE = "HomeScreenLaneRope"
  const val PASS_WATER = "HomeScreenPassWater"
  const val PASS_LEVEL = "HomeScreenPassLevel"
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
  val activeSubscriptionUsedEntries by viewModel.activeSubscriptionUsedEntries.collectAsState()
  val totalSpent by viewModel.totalSpent.collectAsState()
  val remainingCooldown = rememberRemainingCooldown(lastEntryTimestamp)

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  PoolScaffold(
      topBar = {
        TopNavigationMenu(
            Screen.Home,
            actions = {
              AccountButton(
                  onError = { message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                  },
              )
            },
        )
      },
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
        val fabShape = MaterialTheme.shapes.large
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
            shape = fabShape,
            containerColor =
                if (cooldown != null) MaterialTheme.colorScheme.surfaceVariant
                else Color.Transparent,
            contentColor =
                if (cooldown != null) MaterialTheme.colorScheme.onSurfaceVariant
                else PoolPalette.OnDeep,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier =
                Modifier.then(
                        if (cooldown != null) Modifier
                        else Modifier.deepWater(MaterialTheme.colorScheme, fabShape)
                    )
                    .testTag(HomeScreenTestTags.OPEN_SUBSCRIPTION_FAB),
        ) {
          Icon(QrCodeIcon, contentDescription = "Open subscription")
        }
      },
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      // The depth marks of the pool's wall, behind the content along the right edge.
      DepthGauge(
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .fillMaxHeight()
                  .width(14.dp)
                  .padding(vertical = 12.dp)
                  .testTag(HomeScreenTestTags.DEPTH_GAUGE),
      )
      HomeContent(
          stats = stats,
          activeSubscription = activeSubscription,
          activeSubscriptionUsedEntries = activeSubscriptionUsedEntries,
          totalSpent = totalSpent,
          onAddSubscription = { navigationActions?.navigateTo(Screen.Subscription) },
      )
    }
  }
}

/** Home's scrolling column of cards, over the pool floor. */
@Composable
private fun HomeContent(
    stats: HomeStats,
    activeSubscription: Subscription?,
    activeSubscriptionUsedEntries: Int,
    totalSpent: Double,
    onAddSubscription: () -> Unit,
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    LastSwimHero(
        daysSinceLastSwim = stats.daysSinceLastSwim,
        modifier = Modifier.testTag(HomeScreenTestTags.LAST_SWIM_HERO),
    )

    if (activeSubscription != null) {
      ExpirationBanner(
          subscription = activeSubscription,
          usedCount = activeSubscriptionUsedEntries,
          modifier = Modifier.testTag(HomeScreenTestTags.EXPIRATION_BANNER),
      )
    } else {
      NoSubscriptionTodoCard(
          onClick = onAddSubscription,
          modifier = Modifier.testTag(HomeScreenTestTags.NO_SUBSCRIPTION_TODO),
      )
    }

    LaneRope(
        modifier = Modifier.padding(horizontal = 2.dp).testTag(HomeScreenTestTags.LANE_ROPE),
        animated = true,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "This week",
          icon = Icons.Outlined.DateRange,
          value = "${stats.entriesThisWeek}",
          accentColor = PoolPalette.AccentDeep,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_WEEK_STAT),
      )
      StatTile(
          label = "This month",
          icon = Icons.Outlined.DateRange,
          value = "${stats.entriesThisMonth}",
          accentColor = PoolPalette.AccentTeal,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_MONTH_STAT),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "Week streak",
          icon = Icons.Outlined.Refresh,
          value = streakLabel(stats.currentStreakWeeks),
          accentColor = PoolPalette.AccentSun,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.STREAK_STAT),
      )
      StatTile(
          label = "Total entries",
          icon = Icons.Filled.CheckCircle,
          value = "${stats.totalEntries}",
          accentColor = PoolPalette.AccentShallow,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_ENTRIES_STAT),
      )
    }
    WeeklyChartCard(
        counts = stats.weeklyCounts,
        weekStarts = stats.weekStarts,
        modifier = Modifier.testTag(HomeScreenTestTags.WEEKLY_CHART),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "Avg. per week",
          icon = Icons.Filled.Star,
          value = averagePerWeekLabel(stats.averageEntriesPerWeek),
          accentColor = PoolPalette.AccentShallow,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.AVERAGE_PER_WEEK_STAT),
      )
      StatTile(
          label = "Best week",
          icon = Icons.Filled.Star,
          value = if (stats.bestWeekEntries == 0) "-" else "${stats.bestWeekEntries}",
          accentColor = PoolPalette.AccentSun,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.BEST_WEEK_STAT),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "Total spent",
          icon = Icons.Outlined.ShoppingCart,
          value = amountLabel(totalSpent),
          accentColor = PoolPalette.AccentTeal,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_SPENT_STAT),
      )
      StatTile(
          label = "Cost / entry",
          icon = Icons.Outlined.Info,
          value = amountLabel(costPerEntry(totalSpent, stats.totalEntries)),
          accentColor = PoolPalette.AccentDeep,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.COST_PER_ENTRY_STAT),
      )
    }

    stats.favoriteDayOfWeek?.let { day ->
      FavoriteDayCard(day, Modifier.testTag(HomeScreenTestTags.FAVORITE_DAY_BANNER))
    }
  }
}
