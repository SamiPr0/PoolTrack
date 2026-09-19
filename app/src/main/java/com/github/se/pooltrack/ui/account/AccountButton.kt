package com.github.se.pooltrack.ui.account

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
import androidx.lifecycle.viewmodel.compose.viewModel

object AccountButtonTestTags {
  const val BUTTON = "AccountButtonButton"
  const val SIGN_OUT_DIALOG = "AccountButtonSignOutDialog"
  const val SIGN_OUT_BUTTON = "AccountButtonSignOutButton"
}

/**
 * Top-bar account control: signed out, it starts Google sign-in (needed to back subscriptions and
 * entries up to Firestore); signed in, it offers to sign back out. [onError] surfaces a failed or
 * cancelled sign-in attempt, e.g. as a Snackbar.
 */
@Composable
fun RowScope.AccountButton(onError: (String) -> Unit, viewModel: AccountViewModel = viewModel()) {
  val currentUser by viewModel.currentUser.collectAsState()
  val context = LocalContext.current
  var showAccountDialog by remember { mutableStateOf(false) }

  IconButton(
      onClick = {
        if (currentUser == null) viewModel.onSignInClick(context, onError)
        else showAccountDialog = true
      },
      modifier = Modifier.testTag(AccountButtonTestTags.BUTTON),
  ) {
    Icon(
        imageVector = Icons.Filled.Person,
        contentDescription =
            if (currentUser == null) "Sign in to back up your data" else "Account",
    )
  }

  val user = currentUser
  if (showAccountDialog && user != null) {
    AlertDialog(
        onDismissRequest = { showAccountDialog = false },
        modifier = Modifier.testTag(AccountButtonTestTags.SIGN_OUT_DIALOG),
        title = { Text("Signed in") },
        text = {
          Text(
              "Signed in as ${user.displayName ?: user.email ?: "your Google account"}. " +
                  "Subscriptions and entries are backed up to the cloud.")
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
