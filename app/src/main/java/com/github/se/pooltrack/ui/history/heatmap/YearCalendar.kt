package com.github.se.pooltrack.ui.history.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

object YearCalendarTestTags {
  const val CALENDAR = "YearCalendar"
  const val TITLE = "YearCalendarTitle"
  const val PREVIOUS = "YearCalendarPrevious"
  const val NEXT = "YearCalendarNext"

  fun month(month: YearMonth) = "YearCalendarMonth_$month"
}

private const val COLUMNS = 3
private val TileShape = RoundedCornerShape(14.dp)

/**
 * The zoomed-out calendar: a year as twelve tiles, each shaded by how often the user went that
 * month and showing the count. Tap a month to zoom in on it; swipe or use the buttons to change
 * year.
 *
 * @param years The years that can be shown, oldest first; it opens on [initialYear].
 * @param countsByDay Entries per day, see [entryCountsByDay].
 * @param today The current day; months after it are dimmed and can't be opened.
 * @param onMonthClick Called with the tapped month.
 * @param onYearChanged Called with the year on screen, on opening and after every swipe.
 */
@Composable
fun YearCalendar(
    years: List<Int>,
    countsByDay: Map<LocalDate, Int>,
    today: LocalDate,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    initialYear: Int = years.last(),
    onYearChanged: (Int) -> Unit = {},
    locale: Locale = Locale.getDefault(),
) {
  require(years.isNotEmpty()) { "years must not be empty" }
  val initialPage = years.indexOf(initialYear).takeIf { it >= 0 } ?: years.lastIndex
  val pagerState = rememberPagerState(initialPage = initialPage) { years.size }
  val scope = rememberCoroutineScope()
  val monthCounts = remember(countsByDay) { monthCounts(countsByDay) }
  val maxCount = remember(monthCounts) { monthCounts.values.maxOrNull() ?: 0 }
  val year = years[pagerState.currentPage]
  LaunchedEffect(year) { onYearChanged(year) }

  Column(modifier = modifier.fillMaxWidth().testTag(YearCalendarTestTags.CALENDAR)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(YearCalendarTestTags.TITLE),
        )
        Text(
            text = visitsLabel(monthCounts.filterKeys { it.year == year }.values.sum()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(
          onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
          enabled = pagerState.currentPage > 0,
          modifier = Modifier.testTag(YearCalendarTestTags.PREVIOUS),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
      }
      IconButton(
          onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
          enabled = pagerState.currentPage < years.lastIndex,
          modifier = Modifier.testTag(YearCalendarTestTags.NEXT),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
      }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { page ->
      val tiles =
          remember(years[page], monthCounts, today, maxCount) {
            buildYearTiles(years[page], monthCounts, today, maxCount)
          }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(COLUMNS).forEach { rowTiles ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowTiles.forEach { tile ->
              MonthTileCell(
                  tile = tile,
                  today = today,
                  locale = locale,
                  onClick = { onMonthClick(tile.month) },
                  modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MonthTileCell(
    tile: MonthTile,
    today: LocalDate,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val isThisMonth = tile.month == YearMonth.from(today)
  val name = tile.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
  val textColor =
      when {
        tile.isFuture -> scheme.onSurface.copy(alpha = 0.3f)
        else -> scheme.onSurface
      }
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          modifier
              .aspectRatio(1.35f)
              .background(
                  if (tile.level == 0) scheme.onSurface.copy(alpha = 0.06f)
                  else heatmapColor(tile.level, YearPalette),
                  TileShape,
              )
              .then(
                  if (isThisMonth) Modifier.border(1.5.dp, scheme.primary, TileShape) else Modifier
              )
              .then(if (tile.isFuture) Modifier else Modifier.clickable(onClick = onClick))
              .semantics {
                contentDescription = "$name ${tile.month.year}: ${visitsLabel(tile.count)}"
              }
              .testTag(YearCalendarTestTags.month(tile.month)),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = if (isThisMonth) FontWeight.Bold else FontWeight.Medium,
          color = textColor,
          textAlign = TextAlign.Center,
      )
      Text(
          text = if (tile.isFuture) "" else tile.count.toString(),
          style = MaterialTheme.typography.labelMedium,
          color = textColor,
      )
    }
  }
}
