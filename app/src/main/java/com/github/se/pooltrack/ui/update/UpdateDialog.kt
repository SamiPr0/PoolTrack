package com.github.se.pooltrack.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.pooltrack.model.update.AppUpdate
import java.util.Locale

object UpdateDialogTestTags {
  const val DIALOG_TITLE = "UpdateDialogTitle"
  const val VERSION = "UpdateDialogVersion"
  const val NOTES = "UpdateDialogNotes"
  const val PROGRESS = "UpdateDialogProgress"
  const val MESSAGE = "UpdateDialogMessage"
  const val CONFIRM_BUTTON = "UpdateDialogConfirmButton"
  const val DISMISS_BUTTON = "UpdateDialogDismissButton"
}

private const val BYTES_PER_MB = 1024.0 * 1024.0
private const val PERCENT = 100

/**
 * Prompts the user to update when a newer release exists, and guides them through the download and
 * the installation. Draws nothing while there is no update.
 */
@Composable
fun UpdateDialog(viewModel: UpdateViewModel = viewModel()) {
  val state by viewModel.state.collectAsState()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
  UpdateDialogContent(
      state = state,
      onConfirm = {
        if (state is UpdateUiState.ReadyToInstall) viewModel.onInstallClick()
        else viewModel.onUpdateClick()
      },
      onDismiss = viewModel::onDismiss,
  )
}

/** The stateless part of [UpdateDialog]: one dialog whose content follows [state]. */
@Composable
fun UpdateDialogContent(state: UpdateUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  when (state) {
    UpdateUiState.None -> Unit
    is UpdateUiState.Available ->
        Dialog(
            title = "Update available",
            confirmText = "Update",
            dismissText = "Later",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        ) {
          VersionAndNotes(state.update)
        }
    is UpdateUiState.Downloading ->
        Dialog(
            title = "Downloading update",
            confirmText = null,
            dismissText = "Cancel",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            // Tapping outside must not interrupt a download by accident; "Cancel" does.
            dismissOnOutsideTap = false,
        ) {
          Progress(state.progress)
        }
    is UpdateUiState.ReadyToInstall ->
        Dialog(
            title = "Ready to install",
            confirmText = "Install",
            dismissText = "Later",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        ) {
          Message(
              if (state.needsPermission) {
                "Allow PoolTrack to install apps in the settings that just opened, then come " +
                    "back here."
              } else {
                "PoolTrack will close to finish the update. Open it again afterwards."
              }
          )
        }
    is UpdateUiState.Failed ->
        Dialog(
            title = "Update failed",
            confirmText = "Retry",
            dismissText = "Close",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        ) {
          Message(state.message)
        }
  }
}

@Composable
private fun Dialog(
    title: String,
    confirmText: String?,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissOnOutsideTap: Boolean = true,
    content: @Composable () -> Unit,
) {
  AlertDialog(
      onDismissRequest = { if (dismissOnOutsideTap) onDismiss() },
      title = { Text(title, modifier = Modifier.testTag(UpdateDialogTestTags.DIALOG_TITLE)) },
      text = content,
      confirmButton = {
        if (confirmText != null) {
          TextButton(
              onClick = onConfirm,
              modifier = Modifier.testTag(UpdateDialogTestTags.CONFIRM_BUTTON),
          ) {
            Text(confirmText)
          }
        }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(UpdateDialogTestTags.DISMISS_BUTTON),
        ) {
          Text(dismissText)
        }
      },
  )
}

@Composable
private fun VersionAndNotes(update: AppUpdate) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val size = if (update.sizeBytes > 0) " (${formatSize(update.sizeBytes)})" else ""
    Text(
        "Version ${update.version}$size",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.testTag(UpdateDialogTestTags.VERSION),
    )
    val notes = update.notes.toPlainNotes()
    if (notes.isNotEmpty()) {
      Column(modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
        Text(
            notes,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(UpdateDialogTestTags.NOTES),
        )
      }
    }
  }
}

@Composable
private fun Progress(progress: Float?) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val modifier = Modifier.fillMaxWidth().testTag(UpdateDialogTestTags.PROGRESS)
    if (progress == null) {
      LinearProgressIndicator(modifier = modifier)
      Text("Starting download…")
    } else {
      LinearProgressIndicator(progress = { progress }, modifier = modifier)
      Text("${(progress * PERCENT).toInt()}%")
    }
  }
}

@Composable
private fun Message(text: String) {
  Text(text, modifier = Modifier.testTag(UpdateDialogTestTags.MESSAGE))
}

/** Formats [bytes] in megabytes with one decimal, e.g. `12.3 MB`. */
internal fun formatSize(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f MB", bytes / BYTES_PER_MB)

/**
 * Turns GitHub's Markdown release notes into plain text the dialog can show: headings and bold
 * markers are dropped and list items get a bullet.
 */
internal fun String.toPlainNotes(): String =
    lines()
        .map { line ->
          line
              .trim()
              .replace(Regex("^#{1,6}\\s*"), "")
              .replace(Regex("^[-*]\\s+"), "• ")
              .replace("**", "")
              .replace("__", "")
        }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
