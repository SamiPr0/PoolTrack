package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.daysUntilExpiration
import com.github.se.pooltrack.model.subscription.expiresAt
import com.github.se.pooltrack.model.subscription.isExpired
import com.github.se.pooltrack.ui.account.AccountButton
import com.github.se.pooltrack.ui.navigation.BottomNavigationMenu
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.Tab
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import com.github.se.pooltrack.ui.subscription.formatCooldown
import com.github.se.pooltrack.ui.subscription.rememberRemainingCooldown
import com.github.se.pooltrack.ui.theme.DeepWater
import com.github.se.pooltrack.ui.theme.DepthGauge
import com.github.se.pooltrack.ui.theme.LaneRope
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.WaterFill
import com.github.se.pooltrack.ui.theme.deepWater
import com.github.se.pooltrack.ui.theme.deepWaterBrush
import com.github.se.pooltrack.ui.theme.isDarkScheme
import com.github.se.pooltrack.ui.theme.poolTiles
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

  Scaffold(
      modifier = Modifier.poolTiles(MaterialTheme.colorScheme),
      containerColor = Color.Transparent,
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
          value = "${stats.entriesThisWeek}",
          accentColor = PoolPalette.AccentDeep,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_WEEK_STAT),
      )
      StatTile(
          label = "This month",
          value = "${stats.entriesThisMonth}",
          accentColor = PoolPalette.AccentTeal,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.THIS_MONTH_STAT),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "Week streak",
          value = streakLabel(stats.currentStreakWeeks),
          accentColor = PoolPalette.AccentSun,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.STREAK_STAT),
      )
      StatTile(
          label = "Total entries",
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
          value = averagePerWeekLabel(stats.averageEntriesPerWeek),
          accentColor = PoolPalette.AccentShallow,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.AVERAGE_PER_WEEK_STAT),
      )
      StatTile(
          label = "Best week",
          value = if (stats.bestWeekEntries == 0) "-" else "${stats.bestWeekEntries}",
          accentColor = PoolPalette.AccentSun,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.BEST_WEEK_STAT),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatTile(
          label = "Total spent",
          value = amountLabel(totalSpent),
          accentColor = PoolPalette.AccentTeal,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_SPENT_STAT),
      )
      StatTile(
          label = "Cost / entry",
          value = amountLabel(costPerEntry(totalSpent, stats.totalEntries)),
          accentColor = PoolPalette.AccentDeep,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.COST_PER_ENTRY_STAT),
      )
    }

    stats.favoriteDayOfWeek?.let { day ->
      val dayName = day.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
      Card(
          colors = tileColors(),
          modifier = Modifier.fillMaxWidth().testTag(HomeScreenTestTags.FAVORITE_DAY_BANNER),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          IconBadge(
              icon = Icons.Outlined.Favorite,
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          )
          Text(
              text = "You usually swim on $dayName",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(start = 12.dp),
          )
        }
      }
    }
  }
}

/** The plain white tile every card of Home sits on, like a tile of the pool's floor. */
@Composable
private fun tileColors() =
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
private fun LastSwimHero(daysSinceLastSwim: Long?, modifier: Modifier = Modifier) {
  DeepWater(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
      Text(
          text = lastSwimValueLabel(daysSinceLastSwim),
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
      )
      Text(
          text = "since your last swim",
          style = MaterialTheme.typography.bodyMedium,
          color = PoolPalette.OnDeepVariant,
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
private fun ExpirationBanner(
    subscription: Subscription,
    usedCount: Int,
    modifier: Modifier = Modifier,
) {
  val days = subscription.daysUntilExpiration()
  val remainingEntries = subscription.maxEntries?.let { (it - usedCount).coerceAtLeast(0) }
  val message =
      when {
        subscription.expiresAt != null -> expirationMessage(subscription)
        remainingEntries != null ->
            "$remainingEntries of ${subscription.maxEntries} " +
                "${if (subscription.maxEntries == 1) "entry" else "entries"} left"
        else -> "Your active pass has no expiration"
      }
  val isUrgent =
      subscription.isExpired ||
          (days != null && days in 0..7) ||
          (remainingEntries != null && remainingEntries <= 2)
  val badgeColor =
      if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
  val onBadgeColor =
      if (isUrgent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
  val level = passWaterLevel(subscription, usedCount)
  val water = if (isUrgent) PoolPalette.WaterUrgent else PoolPalette.Water
  // In the dark theme the water only tints the card, so the light text stays readable on it.
  val waterColors =
      if (isDarkScheme(MaterialTheme.colorScheme)) water.map { it.copy(alpha = 0.3f) } else water

  Card(modifier = modifier.fillMaxWidth(), colors = tileColors()) {
    Box {
      if (level != null) {
        WaterFill(
            level = level,
            colors = waterColors,
            modifier = Modifier.matchParentSize().testTag(HomeScreenTestTags.PASS_WATER),
        )
      }
      Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        IconBadge(
            icon = if (isUrgent) Icons.Filled.Lock else Icons.Outlined.Notifications,
            containerColor = badgeColor,
            contentColor = onBadgeColor,
            size = 32.dp,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        if (level != null) {
          Text(
              text = passWaterLabel(level),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(start = 8.dp).testTag(HomeScreenTestTags.PASS_LEVEL),
          )
        }
      }
    }
  }
}

/**
 * Rounds the time until/since [subscription]'s expiration to its largest sensible unit - "in 1
 * year" or "expired 2 months ago" rather than the exact (and much less readable) day count.
 */
private fun expirationMessage(subscription: Subscription): String {
  val zone = ZoneId.systemDefault()
  val today = LocalDate.now(zone)
  val expiryDate = subscription.expiresAt!!.atZone(zone).toLocalDate()
  return when {
    expiryDate.isEqual(today) -> "Your active pass expires today"
    expiryDate.isAfter(today) -> {
      val period = Period.between(today, expiryDate)
      if (period.years == 0 && period.months == 0 && period.days == 1) {
        "Your active pass expires tomorrow"
      } else {
        "Your active pass expires in ${humanizePeriod(period)}"
      }
    }
    else -> "Your active pass expired ${humanizePeriod(Period.between(expiryDate, today))} ago"
  }
}

/** The largest non-zero unit of a non-negative [period], pluralized: "1 year", "11 months". */
private fun humanizePeriod(period: Period): String {
  val amount =
      when {
        period.years > 0 -> period.years
        period.months > 0 -> period.months
        else -> period.days
      }
  val unit =
      when {
        period.years > 0 -> "year"
        period.months > 0 -> "month"
        else -> "day"
      }
  return "$amount $unit${if (amount == 1) "" else "s"}"
}

/** A to-do-style prompt shown on Home whenever there's no active subscription to track with. */
@Composable
private fun NoSubscriptionTodoCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      IconBadge(
          icon = Icons.Outlined.AddCircle,
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
      )
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

/**
 * The actual average cost per pool visit so far - [totalSpent] divided by every entry ever
 * confirmed, not tied to a single subscription's plan. Unlike a fixed price/maxEntries ratio, this
 * moves every time a new entry is recorded.
 */
private fun costPerEntry(totalSpent: Double, totalEntries: Int): Double? =
    if (totalSpent == 0.0 || totalEntries == 0) null else totalSpent / totalEntries

/** "29.90", or "-" if [amount] is `null` or zero (i.e. nothing meaningful was recorded). */
private fun amountLabel(amount: Double?): String =
    if (amount == null || amount == 0.0) "-" else String.format(Locale.getDefault(), "%.2f", amount)

private fun averagePerWeekLabel(averageEntriesPerWeek: Double?): String =
    if (averageEntriesPerWeek == null) "-"
    else String.format(Locale.getDefault(), "%.1f", averageEntriesPerWeek)

private fun streakLabel(weeks: Int): String =
    when (weeks) {
      0 -> "-"
      1 -> "1 week"
      else -> "$weeks weeks"
    }

/**
 * A small bar chart of entries per week over the last [RECENT_WEEKS] weeks. Tapping a bar shows
 * which week it is ([weekStarts] holds each bar's Monday); tapping it again hides that again.
 */
@Composable
private fun WeeklyChartCard(
    counts: List<Int>,
    weekStarts: List<LocalDate>,
    modifier: Modifier = Modifier,
) {
  var selected by rememberSaveable { mutableStateOf<Int?>(null) }
  val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
  Card(modifier = modifier.fillMaxWidth(), colors = tileColors()) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(
          text = "Last $RECENT_WEEKS weeks",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
      )
      Row(
          modifier = Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom,
      ) {
        counts.forEachIndexed { index, count ->
          val isCurrent = index == counts.lastIndex
          Column(
              modifier =
                  Modifier.weight(1f)
                      .fillMaxHeight()
                      .testTag(HomeScreenTestTags.WEEKLY_CHART_BAR)
                      .clickable(enabled = index in weekStarts.indices) {
                        selected = if (selected == index) null else index
                      },
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
                text = if (count > 0) "$count" else "",
                style = MaterialTheme.typography.labelSmall,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(0.8f * count / maxCount)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isCurrent || index == selected) {
                              deepWaterBrush(MaterialTheme.colorScheme)
                            } else {
                              SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            }
                        )
            )
          }
        }
      }
      val selectedIndex = selected
      if (selectedIndex != null && selectedIndex in weekStarts.indices) {
        Text(
            text = weekLegend(weekStarts[selectedIndex], counts[selectedIndex]),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp).testTag(HomeScreenTestTags.WEEKLY_CHART_LEGEND),
        )
      }
    }
  }
}

/** "12 Oct - 18 Oct: 3 entries" for the week starting on [weekStart] (a Monday). */
internal fun weekLegend(
    weekStart: LocalDate,
    count: Int,
    locale: Locale = Locale.getDefault(),
): String {
  val format = DateTimeFormatter.ofPattern("d MMM", locale)
  val range = "${weekStart.format(format)} - ${weekStart.plusDays(6).format(format)}"
  return "$range: ${if (count == 1) "1 entry" else "$count entries"}"
}

/** One number of Home, on a white tile edged with [accentColor] like a mosaic tile. */
@Composable
private fun StatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier, shape = MaterialTheme.shapes.small, colors = tileColors()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accentColor))
    }
  }
}

/** A small colored circle behind an icon - the recurring "accent" element across Home's cards. */
@Composable
private fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
  Box(
      modifier = modifier.size(size).clip(CircleShape).background(containerColor),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(size / 2),
    )
  }
}
