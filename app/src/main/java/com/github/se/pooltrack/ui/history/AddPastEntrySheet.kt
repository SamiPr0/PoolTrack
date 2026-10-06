package com.github.se.pooltrack.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object AddPastEntrySheetTestTags {
  const val SHEET = "AddPastEntrySheet"
  const val TODAY_CHIP = "AddPastEntrySheetTodayChip"
  const val YESTERDAY_CHIP = "AddPastEntrySheetYesterdayChip"
  const val DATE_BUTTON = "AddPastEntrySheetDateButton"
  const val TIME_BUTTON = "AddPastEntrySheetTimeButton"
  const val ERROR = "AddPastEntrySheetError"
  const val ADD_BUTTON = "AddPastEntrySheetAddButton"
}

/**
 * A bottom sheet for adding an entry that happened in the past: pick a day (with Today / Yesterday
 * shortcuts) and a time, then add it, all on one screen.
 *
 * @param initialDate The day first shown, e.g. the last one the user added.
 * @param initialTime The time first shown.
 * @param error Why the last attempt was refused, shown inline; `null` when there is nothing to say.
 * @param onInputChanged Called when the user edits the day or time, so a stale [error] can go.
 * @param onAdd Called with the chosen day and time (in [zone]) when the user taps "Add entry".
 * @param onDismiss Called when the sheet is dismissed without adding.
 * @param zone The zone the chosen day and time are in; the device's, outside tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastEntrySheet(
    initialDate: LocalDate,
    initialTime: LocalTime,
    error: AddPastEntryError?,
    onInputChanged: () -> Unit,
    onAdd: (LocalDate, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
  var date by remember { mutableStateOf(initialDate) }
  var time by remember { mutableStateOf(initialTime) }
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  val today = LocalDate.now(zone)
  val locale = LocalConfiguration.current.locales[0]

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      modifier = Modifier.testTag(AddPastEntrySheetTestTags.SHEET),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Add a past entry", style = MaterialTheme.typography.titleLarge)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = date == today,
            onClick = {
              date = today
              onInputChanged()
            },
            label = { Text("Today") },
            modifier = Modifier.testTag(AddPastEntrySheetTestTags.TODAY_CHIP),
        )
        FilterChip(
            selected = date == today.minusDays(1),
            onClick = {
              date = today.minusDays(1)
              onInputChanged()
            },
            label = { Text("Yesterday") },
            modifier = Modifier.testTag(AddPastEntrySheetTestTags.YESTERDAY_CHIP),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f).testTag(AddPastEntrySheetTestTags.DATE_BUTTON),
        ) {
          Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)))
        }
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.testTag(AddPastEntrySheetTestTags.TIME_BUTTON),
        ) {
          Text(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)))
        }
      }

      if (error != null) {
        Text(
            text = error.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(AddPastEntrySheetTestTags.ERROR),
        )
      }

      Button(
          onClick = { onAdd(date, time) },
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = 24.dp)
                  .testTag(AddPastEntrySheetTestTags.ADD_BUTTON),
      ) {
        Text("Add entry")
      }
    }
  }

  if (showDatePicker) {
    // The picker works in UTC, so the selection is anchored there both ways (as in the
    // subscription form). Days after today are not selectable: they can't have happened yet.
    val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                  override fun isSelectableDate(utcTimeMillis: Long) =
                      utcTimeMillis <= todayUtcMillis
                },
        )
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                  onInputChanged()
                }
                showDatePicker = false
              },
          ) {
            Text("OK")
          }
        },
        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState =
        rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
          TextButton(
              onClick = {
                time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                onInputChanged()
                showTimePicker = false
              },
          ) {
            Text("OK")
          }
        },
        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
    )
  }
}
