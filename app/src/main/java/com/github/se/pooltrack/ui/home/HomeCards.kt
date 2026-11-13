package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.daysUntilExpiration
import com.github.se.pooltrack.model.subscription.expiresAt
import com.github.se.pooltrack.model.subscription.isExpired
import com.github.se.pooltrack.ui.theme.DeepWater
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.WaterFill
import com.github.se.pooltrack.ui.theme.isDarkScheme
import com.github.se.pooltrack.ui.theme.poolTileColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// The cards and tiles of Home, laid out by HomeScreen.

/** How long since the last swim, in big type on deep water. */
@Composable
internal fun LastSwimHero(daysSinceLastSwim: Long?, modifier: Modifier = Modifier) {
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

/** "Today", "1 day", "5 days", or "No entries yet" before the first swim. */
private fun lastSwimValueLabel(daysSinceLastSwim: Long?): String =
    when (daysSinceLastSwim) {
      null -> "No entries yet"
      0L -> "Today"
      1L -> "1 day"
      else -> "$daysSinceLastSwim days"
    }

/**
 * The active pass: its expiration or remaining entries, and water filling the card up to what is
 * left of it ([passWaterLevel]). Red once it's urgent or past.
 */
@Composable
internal fun ExpirationBanner(
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

  Card(modifier = modifier.fillMaxWidth(), colors = poolTileColors()) {
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
internal fun NoSubscriptionTodoCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun costPerEntry(totalSpent: Double, totalEntries: Int): Double? =
    if (totalSpent == 0.0 || totalEntries == 0) null else totalSpent / totalEntries

/** "29.90", or "-" if [amount] is `null` or zero (i.e. nothing meaningful was recorded). */
internal fun amountLabel(amount: Double?): String =
    if (amount == null || amount == 0.0) "-" else String.format(Locale.getDefault(), "%.2f", amount)

/** "2.5" entries per week, or "-" before there is anything to average. */
internal fun averagePerWeekLabel(averageEntriesPerWeek: Double?): String =
    if (averageEntriesPerWeek == null) "-"
    else String.format(Locale.getDefault(), "%.1f", averageEntriesPerWeek)

/** "1 week", "3 weeks", or "-" without a streak. */
internal fun streakLabel(weeks: Int): String =
    when (weeks) {
      0 -> "-"
      1 -> "1 week"
      else -> "$weeks weeks"
    }

/** One number of Home, on a white tile edged with [accentColor] like a mosaic tile. */
@Composable
internal fun StatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier, shape = MaterialTheme.shapes.small, colors = poolTileColors()) {
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
internal fun IconBadge(
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

/** The weekday the user swims on most, e.g. "You usually swim on Tuesday". */
@Composable
internal fun FavoriteDayCard(day: DayOfWeek, modifier: Modifier = Modifier) {
  val dayName = day.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
  Card(colors = poolTileColors(), modifier = modifier.fillMaxWidth()) {
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
