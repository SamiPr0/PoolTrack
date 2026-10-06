package com.github.se.pooltrack.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object AccountButtonTestTags {
  const val BUTTON = "AccountButtonButton"
  const val SIGN_OUT_DIALOG = "AccountButtonSignOutDialog"
  const val SIGN_OUT_BUTTON = "AccountButtonSignOutButton"
}

/**
 * Home's account control, showing who's signed in and offering to sign out. Reaching Home at all
 * already requires a real (non-anonymous) Google account - see `PoolTrackApp` in
 * `MainActivity.kt` - so the "not signed in" branch here is only a defensive fallback, e.g. for the
 * brief moment right after tapping sign-out. [onError] surfaces a failed sign-in attempt from that
 * fallback, e.g. as a Snackbar.
 */
@Composable
fun RowScope.AccountButton(onError: (String) -> Unit, viewModel: AccountViewModel = viewModel()) {
  val currentUser by viewModel.currentUser.collectAsState()
  val context = LocalContext.current
  var showAccountDialog by remember { mutableStateOf(false) }

  val user = currentUser
  val isRealAccount = user != null && !user.isAnonymous

  IconButton(
      onClick = {
        if (isRealAccount) showAccountDialog = true else viewModel.onSignInClick(context, onError)
      },
      modifier = Modifier.testTag(AccountButtonTestTags.BUTTON),
  ) {
    Icon(
        imageVector = Icons.Filled.Person,
        contentDescription = if (isRealAccount) "Account" else "Sign in to back up your data",
    )
  }

  if (showAccountDialog && user != null) {
    AlertDialog(
        onDismissRequest = { showAccountDialog = false },
        modifier = Modifier.testTag(AccountButtonTestTags.SIGN_OUT_DIALOG),
        title = { Text("Signed in") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Signed in as ${user.displayName ?: user.email ?: "your Google account"}. " +
                    "Subscriptions and entries are backed up to the cloud."
            )
            AppVersionText()
          }
        },
        confirmButton = {
          TextButton(
              onClick = {
                viewModel.onSignOutClick()
                showAccountDialog = false
              },
              modifier = Modifier.testTag(AccountButtonTestTags.SIGN_OUT_BUTTON),
          ) {
            Text("Sign out")
          }
        },
        dismissButton = { TextButton(onClick = { showAccountDialog = false }) { Text("Close") } },
    )
  }
}
