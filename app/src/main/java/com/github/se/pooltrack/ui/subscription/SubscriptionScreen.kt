package com.github.se.pooltrack.ui.subscription

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SubscriptionScreenTestTags {
  const val PICK_BUTTON = "SubscriptionScreenPickButton"
  const val PDF_IMAGE = "SubscriptionScreenPdfImage"
  const val ACCEPT_BUTTON = "SubscriptionScreenAcceptButton"
  const val DECLINE_BUTTON = "SubscriptionScreenDeclineButton"
}

/**
 * SubscriptionScreen displays the user's subscription PDF at maximum screen brightness, so it can
 * be scanned at the pool entrance. If no PDF has been picked yet, it prompts the user to choose
 * one.
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    onEntryConfirmed: () -> Unit = {},
) {
  val subscriptionUri by viewModel.subscriptionUri.collectAsState()
  val context = LocalContext.current

  val pickPdfLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onSubscriptionPicked(uri)
      }

  MaxBrightness()

  val currentUri = subscriptionUri
  if (currentUri == null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(
          onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
          modifier = Modifier.testTag(SubscriptionScreenTestTags.PICK_BUTTON),
      ) {
        Text("Choose subscription PDF")
      }
    }
  } else {
    var pageBitmap by remember(currentUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentUri) {
      pageBitmap = withContext(Dispatchers.IO) { renderFirstPage(context, Uri.parse(currentUri)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      pageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Subscription pass",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier.weight(1f).fillMaxWidth().testTag(SubscriptionScreenTestTags.PDF_IMAGE),
        )
      }

      Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        Button(
            onClick = {
              viewModel.onScannerAccepted()
              onEntryConfirmed()
            },
            modifier = Modifier.testTag(SubscriptionScreenTestTags.ACCEPT_BUTTON),
        ) {
          Text("Scanner accepted")
        }
        Button(
            // A declined scan must not count as an entry, so this stays a no-op: the pass
            // just remains on screen for the user to try scanning again.
            onClick = {},
            modifier = Modifier.testTag(SubscriptionScreenTestTags.DECLINE_BUTTON),
        ) {
          Text("Scanner declined")
        }
      }
    }
  }
}

/** Forces the current activity's screen brightness to maximum while this composable is shown. */
@Composable
private fun MaxBrightness() {
  val activity = LocalContext.current as? Activity ?: return
  DisposableEffect(activity) {
    val window = activity.window
    val originalBrightness = window.attributes.screenBrightness

    val maxParams = window.attributes
    maxParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    window.attributes = maxParams

    onDispose {
      val restoredParams = window.attributes
      restoredParams.screenBrightness = originalBrightness
      window.attributes = restoredParams
    }
  }
}

/** Renders the first page of the PDF at [uri] to a [Bitmap], or `null` if it has no pages. */
private fun renderFirstPage(context: Context, uri: Uri): Bitmap? {
  val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
  pfd.use {
    PdfRenderer(it).use { renderer ->
      if (renderer.pageCount == 0) return null
      renderer.openPage(0).use { page ->
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
      }
    }
  }
}
