package com.github.se.pooltrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.github.se.pooltrack.ui.theme.PoolTrackTheme

object HomeScreenTestTags {
  const val GREETING = "HomeScreenGreeting"
}

/** HomeScreen composable that is the first screen shown when the app launches. */
@Composable
fun HomeScreen() {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Hello World",
        modifier = Modifier.testTag(HomeScreenTestTags.GREETING),
    )
  }
}

// Switch to the `Split` view to see the preview alongside the code.
@Preview
@Composable
fun HomeScreenPreview() {
  PoolTrackTheme { HomeScreen() }
}
