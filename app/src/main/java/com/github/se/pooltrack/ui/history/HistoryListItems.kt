package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** "October" for a month of the current year, "October 2025" for any other. */
internal fun monthLabel(
    month: YearMonth,
    thisMonth: YearMonth,
    locale: Locale = Locale.getDefault(),
): String {
  val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
  return if (month.year == thisMonth.year) name else "$name ${month.year}"
}

/** "5 visits" (or "1 visit"). */
internal fun visitsLabel(count: Int): String = if (count == 1) "1 visit" else "$count visits"

/** The sticky title above all the entries of one month, with how many there were. */
@Composable
internal fun MonthHeader(label: String, visits: Int, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(HistoryScreenTestTags.MONTH_HEADER),
      color = MaterialTheme.colorScheme.background,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
      )
      Text(
          text = visitsLabel(visits),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** A light label above the entries of one day; the count only shows when there are several. */
@Composable
internal fun DayHeader(label: String, entryCount: Int, modifier: Modifier = Modifier) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(top = 12.dp, bottom = 2.dp)
              .testTag(HistoryScreenTestTags.DAY_HEADER),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    if (entryCount > 1) {
      Text(
          text = "$entryCount entries",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/**
 * A flat, compact row for one entry. Tap it to open the entry; swipe it to the left to delete it
 * (the caller offers an Undo). Screen readers get a "Delete entry" action instead of the swipe.
 *
 * @param label The text of the row, see [entryRowLabel].
 * @param onClick Called when the row is tapped.
 * @param onDelete Called once the row was swiped away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableEntryRow(
    label: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val dismissState =
      rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
              onDelete()
              true
            } else false
          }
      )
  SwipeToDismissBox(
      state = dismissState,
      enableDismissFromStartToEnd = false,
      modifier =
          modifier.testTag(HistoryScreenTestTags.ENTRY_ITEM).semantics {
            customActions =
                listOf(
                    CustomAccessibilityAction("Delete entry") {
                      onDelete()
                      true
                    }
                )
          },
      backgroundContent = {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd,
        ) {
          Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onError,
              modifier = Modifier.padding(end = 20.dp),
          )
        }
      },
  ) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background) {
      Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
          )
          Text(
              text = label,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
          )
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
    }
  }
}
