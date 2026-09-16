package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.github.se.pooltrack.ui.theme.PoolTrackTheme

object HomeScreenTestTags {
  const val GREETING = "HomeScreenGreeting"
  const val VIEW_SUBSCRIPTION_BUTTON = "HomeScreenViewSubscriptionButton"
  const val VIEW_HISTORY_BUTTON = "HomeScreenViewHistoryButton"
}

/** HomeScreen composable that is the first screen shown when the app launches. */
@Composable
fun HomeScreen(onViewSubscription: () -> Unit = {}, onViewHistory: () -> Unit = {}) {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Hello World",
        modifier = Modifier.testTag(HomeScreenTestTags.GREETING),
    )
    Button(
        onClick = onViewSubscription,
        modifier = Modifier.testTag(HomeScreenTestTags.VIEW_SUBSCRIPTION_BUTTON),
    ) {
      Text("View subscription")
    }
    Button(
        onClick = onViewHistory,
        modifier = Modifier.testTag(HomeScreenTestTags.VIEW_HISTORY_BUTTON),
    ) {
      Text("View entry history")
    }
  }
}

// Switch to the `Split` view to see the preview alongside the code.
@Preview
@Composable
fun HomeScreenPreview() {
  PoolTrackTheme { HomeScreen() }
}
