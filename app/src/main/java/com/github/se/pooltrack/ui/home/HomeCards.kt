package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.pooltrack.ui.theme.DeepWater
import com.github.se.pooltrack.ui.theme.DiveRipples
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.PoolTile
import java.util.Locale

// The cards and tiles of Home, laid out by HomeScreen.

/**
 * How long since the last swim, in big type on deep water, with rings spreading as after a dive.
 */
@Composable
internal fun LastSwimHero(daysSinceLastSwim: Long?, modifier: Modifier = Modifier) {
  DeepWater(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    DiveRipples(Modifier.matchParentSize())
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = "LAST SWIM",
          style = MaterialTheme.typography.labelMedium,
          letterSpacing = 2.sp,
          color = PoolPalette.OnDeepVariant,
      )
      Text(
          text = lastSwimValueLabel(daysSinceLastSwim),
          style = MaterialTheme.typography.displaySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp),
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

/**
 * One number of Home on a tile, with [icon] large, faint and tilted in its corner, half off the
 * tile, in [accentColor] as the tile's motif.
 */
@Composable
internal fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
  PoolTile(modifier = modifier, shape = MaterialTheme.shapes.small) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor.copy(alpha = 0.18f),
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .offset(x = 14.dp, y = 14.dp)
                  .size(64.dp)
                  .rotate(-15f),
      )
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
      }
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

/** What the swims cost: [totalSpent] and the [costPerEntry], side by side on one tile. */
@Composable
internal fun SpendingCard(totalSpent: String, costPerEntry: String, modifier: Modifier = Modifier) {
  PoolTile(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      SpendingHalf(
          label = "Total spent",
          value = totalSpent,
          icon = Icons.Outlined.ShoppingCart,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.TOTAL_SPENT_STAT),
      )
      VerticalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
      )
      SpendingHalf(
          label = "Cost / entry",
          value = costPerEntry,
          icon = Icons.Outlined.Info,
          modifier = Modifier.weight(1f).testTag(HomeScreenTestTags.COST_PER_ENTRY_STAT),
      )
    }
  }
}

/** One amount of [SpendingCard]: a small [icon], the [value] and its [label]. */
@Composable
private fun SpendingHalf(label: String, value: String, icon: ImageVector, modifier: Modifier) {
  Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    InlineIcon(icon)
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
    )
  }
}

/** A small icon in the primary color, leading a line or a figure inside a tile. */
@Composable
internal fun InlineIcon(icon: ImageVector, modifier: Modifier = Modifier) {
  Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = modifier.size(18.dp),
  )
}
