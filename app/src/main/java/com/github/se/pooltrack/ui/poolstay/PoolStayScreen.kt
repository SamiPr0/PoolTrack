package com.github.se.pooltrack.ui.poolstay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.swim.MAX_SWIM_DISTANCE_METERS
import java.time.Duration
import java.util.Locale

object PoolStayScreenTestTags {
  const val ENJOY_MESSAGE = "PoolStayScreenEnjoyMessage"
  const val CANCEL_WINDOW_MESSAGE = "PoolStayScreenCancelWindowMessage"
  const val CANCEL_BUTTON = "PoolStayScreenCancelButton"
  const val LOG_TITLE = "PoolStayScreenLogTitle"
  const val DISTANCE_INPUT = "PoolStayScreenDistanceInput"
  const val SAVE_BUTTON = "PoolStayScreenSaveButton"
}

/**
 * Shown instead of the whole app from the moment a scan is confirmed until the user logged how far
 * they swam, see [PoolStayViewModel]. `PoolTrackApp` decides when to show it.
 */
@Composable
fun PoolStayScreen(viewModel: PoolStayViewModel = viewModel()) {
  val state by viewModel.state.collectAsState()
  val input by viewModel.distanceInput.collectAsState()
  val isValid by viewModel.isDistanceValid.collectAsState()
  PoolStayContent(
      state = state,
      distanceInput = input,
      isDistanceValid = isValid,
      onDistanceChanged = viewModel::onDistanceChanged,
      onCancelEntry = viewModel::onCancelEntry,
      onSaveDistance = viewModel::onSaveDistance,
  )
}

/** The stateless part of [PoolStayScreen]: its content follows [state]. */
@Composable
fun PoolStayContent(
    state: PoolStayState,
    distanceInput: String,
    isDistanceValid: Boolean,
    onDistanceChanged: (String) -> Unit,
    onCancelEntry: () -> Unit,
    onSaveDistance: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    when (state) {
      is PoolStayState.CancelWindow -> CancelWindowContent(state.remaining, onCancelEntry)
      is PoolStayState.LogRequired ->
          LogContent(distanceInput, isDistanceValid, onDistanceChanged, onSaveDistance)
      PoolStayState.Loading,
      PoolStayState.None -> Unit
    }
  }
}

@Composable
private fun CancelWindowContent(remaining: Duration, onCancelEntry: () -> Unit) {
  Icon(
      imageVector = Icons.Filled.Check,
      contentDescription = null,
      modifier = Modifier.size(72.dp),
      tint = MaterialTheme.colorScheme.primary,
  )
  Text(
      text = "You're in. Enjoy your swim!",
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 24.dp).testTag(PoolStayScreenTestTags.ENJOY_MESSAGE),
  )
  Text(
      text =
          "Scanned by mistake? You can still cancel this entry for ${formatCountdown(remaining)}.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier =
          Modifier.padding(top = 12.dp, bottom = 32.dp)
              .testTag(PoolStayScreenTestTags.CANCEL_WINDOW_MESSAGE),
  )
  OutlinedButton(
      onClick = onCancelEntry,
      modifier = Modifier.testTag(PoolStayScreenTestTags.CANCEL_BUTTON),
  ) {
    Text("Cancel entry")
  }
}

@Composable
private fun LogContent(
    distanceInput: String,
    isDistanceValid: Boolean,
    onDistanceChanged: (String) -> Unit,
    onSaveDistance: () -> Unit,
) {
  // The user has to log a distance to get back to the app, so there is no way out with Back.
  BackHandler(enabled = true) {}
  Text(
      text = "How far did you swim?",
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag(PoolStayScreenTestTags.LOG_TITLE),
  )
  OutlinedTextField(
      value = distanceInput,
      onValueChange = onDistanceChanged,
      label = { Text("Distance") },
      suffix = { Text("m") },
      singleLine = true,
      isError = distanceInput.isNotEmpty() && !isDistanceValid,
      supportingText = {
        if (distanceInput.isNotEmpty() && !isDistanceValid) {
          Text("Enter a distance between 1 and $MAX_SWIM_DISTANCE_METERS metres")
        }
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 24.dp, bottom = 16.dp)
              .testTag(PoolStayScreenTestTags.DISTANCE_INPUT),
  )
  Button(
      onClick = onSaveDistance,
      enabled = isDistanceValid,
      modifier = Modifier.testTag(PoolStayScreenTestTags.SAVE_BUTTON),
  ) {
    Text("Save")
  }
}

/** Formats [remaining] as `m:ss`, e.g. `9:05`. */
fun formatCountdown(remaining: Duration): String {
  val seconds = remaining.seconds.coerceAtLeast(0)
  return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
