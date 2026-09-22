package com.github.se.pooltrack.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

object SignInScreenTestTags {
  const val SIGN_IN_BUTTON = "SignInScreenSignInButton"
}

/**
 * Shown instead of the rest of the app whenever there's no signed-in Google account - PoolTrack
 * requires one, since subscriptions and entries are backed up to it. `PoolTrackApp` in
 * `MainActivity.kt` is what decides whether to show this or the real app.
 */
@Composable
fun SignInScreen(viewModel: AccountViewModel = viewModel()) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
          imageVector = Icons.Filled.Person,
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.primary,
      )
      Text(
          text = "Sign in to continue",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
      )
      Text(
          text = "PoolTrack backs up your subscriptions and entries to your Google account.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 24.dp),
      )
      Button(
          onClick = {
            viewModel.onSignInClick(context) { message ->
              coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            }
          },
          modifier = Modifier.testTag(SignInScreenTestTags.SIGN_IN_BUTTON),
      ) {
        Text("Sign in with Google")
      }
    }
  }
}
