package com.github.se.pooltrack.ui.poolstay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.swim.MAX_SWIM_DISTANCE_METERS
import com.github.se.pooltrack.model.swim.POOL_LENGTHS_METERS
import com.github.se.pooltrack.model.swim.changePoolLength
import com.github.se.pooltrack.model.swim.metersToLengths
import com.github.se.pooltrack.model.swim.parseSwimDistance
import com.github.se.pooltrack.model.swim.stepLengths
import com.github.se.pooltrack.ui.theme.Bubbles
import com.github.se.pooltrack.ui.theme.DeepWater
import com.github.se.pooltrack.ui.theme.LaneMarker
import com.github.se.pooltrack.ui.theme.LaneRope
import com.github.se.pooltrack.ui.theme.PoolPalette
import com.github.se.pooltrack.ui.theme.bobbing
import com.github.se.pooltrack.ui.theme.loopingFraction
import com.github.se.pooltrack.ui.theme.poolTileColors
import com.github.se.pooltrack.ui.theme.rememberAnimationsEnabled
import java.time.Duration
import java.util.Locale

object PoolStayScreenTestTags {
  const val ENJOY_MESSAGE = "PoolStayScreenEnjoyMessage"
  const val CANCEL_WINDOW_MESSAGE = "PoolStayScreenCancelWindowMessage"
  const val CANCEL_BUTTON = "PoolStayScreenCancelButton"
  const val LOG_TITLE = "PoolStayScreenLogTitle"
  const val DISTANCE_INPUT = "PoolStayScreenDistanceInput"
  const val SAVE_BUTTON = "PoolStayScreenSaveButton"
  const val LENGTHS_COUNT = "PoolStayScreenLengthsCount"
  const val LENGTHS_PLUS = "PoolStayScreenLengthsPlus"
  const val LENGTHS_MINUS = "PoolStayScreenLengthsMinus"

  /** The chip that picks a pool [meters] long. */
  fun poolLength(meters: Int) = "PoolStayScreenPoolLength_$meters"
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
    // Centred, yet scrollable so the keyboard never hides the Save button on a small phone.
    Column(
        modifier =
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
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
          Modifier.bobbing(bob, amplitude = 3.dp)
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
      LengthCounter(
          distanceInput = distanceInput,
          onDistanceChanged = onDistanceChanged,
          modifier = Modifier.padding(top = 16.dp),
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

/**
 * Counts the swim in lengths of a 25 m or 50 m pool, the way swimmers count it, between two lane
 * ropes, and writes the matching metres into the distance field. That field stays the one source of
 * truth, so the user can still type the metres instead; a distance that is not a whole number of
 * lengths shows a dash.
 */
@Composable
private fun LengthCounter(
    distanceInput: String,
    onDistanceChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  var poolLength by rememberSaveable { mutableIntStateOf(POOL_LENGTHS_METERS.first()) }
  val meters = parseSwimDistance(distanceInput)
  val lengths = metersToLengths(meters, poolLength)
  val write = { newMeters: Int? ->
    onDistanceChanged(if (newMeters == null || newMeters <= 0) "" else newMeters.toString())
  }
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      POOL_LENGTHS_METERS.forEach { length ->
        FilterChip(
            selected = poolLength == length,
            onClick = {
              write(changePoolLength(meters, fromPoolLength = poolLength, toPoolLength = length))
              poolLength = length
            },
            label = { Text("$length m pool") },
            modifier = Modifier.testTag(PoolStayScreenTestTags.poolLength(length)),
        )
      }
    }
    LaneRope(modifier = Modifier.padding(top = 16.dp), animated = false)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedIconButton(
          onClick = { write(stepLengths(meters, poolLength, -1)) },
          modifier = Modifier.size(52.dp).testTag(PoolStayScreenTestTags.LENGTHS_MINUS),
      ) {
        // The core icon set has no minus sign.
        Text(
            text = "−",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = "One length less" },
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = lengths?.toString() ?: if (meters == null) "0" else "–",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(PoolStayScreenTestTags.LENGTHS_COUNT),
        )
        Text(
            text = "lengths",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilledIconButton(
          onClick = { write(stepLengths(meters, poolLength, 1)) },
          modifier = Modifier.size(52.dp).testTag(PoolStayScreenTestTags.LENGTHS_PLUS),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "One more length")
      }
    }
    LaneRope(modifier = Modifier, animated = false)
  }
}

/** Formats [remaining] as `m:ss`, e.g. `9:05`. */
fun formatCountdown(remaining: Duration): String {
  val seconds = remaining.seconds.coerceAtLeast(0)
  return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
