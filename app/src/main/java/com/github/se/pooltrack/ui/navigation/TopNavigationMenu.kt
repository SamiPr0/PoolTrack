package com.github.se.pooltrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Top bar shared by every screen, showing the current screen's name and a back button whenever
 * that screen isn't one of the bottom navigation tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationMenu(currentScreen: Screen, onGoBack: () -> Unit = {}) {
  TopAppBar(
      title = {
        Text(
            text = currentScreen.name,
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
        )
      },
      navigationIcon = {
        if (!currentScreen.isTopLevelDestination) {
          IconButton(
              onClick = onGoBack,
              modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      },
  )
}
