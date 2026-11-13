package com.github.se.pooltrack.ui.poolstay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.swim.MAX_SWIM_DISTANCE_METERS
import com.github.se.pooltrack.ui.theme.Bubbles
import com.github.se.pooltrack.ui.theme.DeepWater
import com.github.se.pooltrack.ui.theme.LaneMarker
import com.github.se.pooltrack.ui.theme.LaneRope
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.loopingFraction
import com.github.se.pooltrack.ui.theme.poolTileColors
import com.github.se.pooltrack.ui.theme.rememberAnimationsEnabled
import java.time.Duration
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

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

/** The stateless part of [PoolStayScreen]: its content follows [state], in the water. */
@Composable
fun PoolStayContent(
    state: PoolStayState,
    distanceInput: String,
    isDistanceValid: Boolean,
    onDistanceChanged: (String) -> Unit,
    onCancelEntry: () -> Unit,
    onSaveDistance: () -> Unit,
) {
  DeepWater(modifier = Modifier.fillMaxSize()) {
    Bubbles(modifier = Modifier.matchParentSize())
    // The lane's line on the floor, seen from above, faint behind the content.
    LaneMarker(
        modifier =
            Modifier.align(Alignment.Center)
                .fillMaxHeight()
                .width(44.dp)
                .padding(vertical = 56.dp)
                .alpha(0.35f)
    )
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
}

@Composable
private fun CancelWindowContent(remaining: Duration, onCancelEntry: () -> Unit) {
  // The check floats on the surface, bobbing gently.
  val bob = loopingFraction(3_000, rememberAnimationsEnabled(), "checkBob")
  Box(
      modifier =
          Modifier.graphicsLayer { translationY = -3.dp.toPx() * sin(bob.value * 2 * PI.toFloat()) }
              .size(88.dp)
              .background(PoolPalette.OnDeep, CircleShape),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = PoolPalette.AccentDeep,
    )
  }
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
      color = PoolPalette.OnDeepVariant,
      textAlign = TextAlign.Center,
      modifier =
          Modifier.padding(top = 12.dp, bottom = 32.dp)
              .testTag(PoolStayScreenTestTags.CANCEL_WINDOW_MESSAGE),
  )
  OutlinedButton(
      onClick = onCancelEntry,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = PoolPalette.OnDeep),
      border = BorderStroke(1.5.dp, PoolPalette.OnDeep),
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
  LaneRope(modifier = Modifier.padding(bottom = 24.dp), animated = true)
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors = poolTileColors(),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = "How far did you swim?",
          style = MaterialTheme.typography.headlineSmall,
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
                  .padding(top = 16.dp, bottom = 8.dp)
                  .testTag(PoolStayScreenTestTags.DISTANCE_INPUT),
      )
      Button(
          onClick = onSaveDistance,
          enabled = isDistanceValid,
          modifier = Modifier.fillMaxWidth().testTag(PoolStayScreenTestTags.SAVE_BUTTON),
      ) {
        Text("Save")
      }
    }
  }
}

/** Formats [remaining] as `m:ss`, e.g. `9:05`. */
fun formatCountdown(remaining: Duration): String {
  val seconds = remaining.seconds.coerceAtLeast(0)
  return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
