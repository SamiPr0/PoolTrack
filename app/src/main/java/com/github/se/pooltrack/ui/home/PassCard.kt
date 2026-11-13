package com.github.se.pooltrack.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.pooltrack.model.subscription.Subscription
import com.github.se.pooltrack.model.subscription.daysUntilExpiration
import com.github.se.pooltrack.model.subscription.expiresAt
import com.github.se.pooltrack.model.subscription.isExpired
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.PoolTile
import com.github.se.pooltrack.ui.theme.WaterFill
import com.github.se.pooltrack.ui.theme.isDarkScheme
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * The active pass: its expiration or remaining entries, water filling the card up to what is left
 * of it ([passWaterLevel]) and, for a pass counted in entries, one capsule per entry
 * ([entryCapsules]). Red once it's urgent or past.
 */
@Composable
internal fun PassCard(
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

  val capsules = entryCapsules(subscription, usedCount)

  PoolTile(modifier = modifier.fillMaxWidth()) {
    Box {
      if (level != null) {
        WaterFill(
            level = level,
            colors = waterColors,
            modifier = Modifier.matchParentSize().testTag(HomeScreenTestTags.PASS_WATER),
        )
      }
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        if (capsules.isNotEmpty()) {
          EntryCapsules(
              capsules = capsules,
              color = badgeColor,
              modifier =
                  Modifier.padding(start = 44.dp, top = 10.dp)
                      .testTag(HomeScreenTestTags.PASS_ENTRY_CAPSULES),
          )
        }
      }
    }
  }
}

/**
 * One capsule per entry of a pass, filled in [color] while still available and hollow once used.
 * They fill in one after another when first shown.
 */
@Composable
private fun EntryCapsules(capsules: List<Boolean>, color: Color, modifier: Modifier = Modifier) {
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    capsules.forEachIndexed { index, available ->
      val appear by
          animateFloatAsState(
              targetValue = if (shown) 1f else 0f,
              animationSpec = tween(durationMillis = 300, delayMillis = 300 + index * 60),
              label = "entryCapsule",
          )
      val shape = RoundedCornerShape(50)
      Box(
          modifier =
              Modifier.weight(1f)
                  .height(8.dp)
                  .graphicsLayer {
                    alpha = appear
                    scaleX = 0.2f + 0.8f * appear
                  }
                  .then(
                      if (available) Modifier.background(color, shape)
                      else Modifier.border(1.dp, color.copy(alpha = 0.5f), shape)
                  ),
      )
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
