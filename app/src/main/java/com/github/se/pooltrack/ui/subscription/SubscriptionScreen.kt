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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.navigation.TopNavigationMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SubscriptionScreenTestTags {
  const val PICK_BUTTON = "SubscriptionScreenPickButton"
  const val LOADING_INDICATOR = "SubscriptionScreenLoadingIndicator"
  const val PDF_IMAGE = "SubscriptionScreenPdfImage"
  const val ACCEPT_BUTTON = "SubscriptionScreenAcceptButton"
  const val REPLACE_BUTTON = "SubscriptionScreenReplaceButton"
}

/**
 * SubscriptionScreen displays the user's subscription PDF at maximum screen brightness, so it can
 * be scanned at the pool entrance. If no PDF has been picked yet, it prompts the user to choose
 * one.
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val subscriptionUri by viewModel.subscriptionUri.collectAsState()
  val context = LocalContext.current

  val pickPdfLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onSubscriptionPicked(uri)
      }

  MaxBrightness()

  Scaffold(
      topBar = {
        TopNavigationMenu(
            Screen.Subscription,
            onGoBack = { navigationActions?.goBack() },
            actions = {
              // Once a subscription is used up (e.g. a monthly pass expired), the user needs a
              // way to swap in a new one without deleting the app data by hand.
              if (subscriptionUri != null) {
                IconButton(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.testTag(SubscriptionScreenTestTags.REPLACE_BUTTON),
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = "Replace subscription")
                }
              }
            },
        )
      },
  ) { paddingValues ->
    val currentUri = subscriptionUri
    if (currentUri == null) {
      Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No subscription yet",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Text(
            text = "Add your pool subscription PDF once, then show it at the scanner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.testTag(SubscriptionScreenTestTags.PICK_BUTTON),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add subscription PDF")
        }
      }
    } else {
      var pageBitmap by remember(currentUri) { mutableStateOf<Bitmap?>(null) }

      LaunchedEffect(currentUri) {
        pageBitmap =
            withContext(Dispatchers.IO) { renderFirstPage(context, Uri.parse(currentUri)) }
      }

      Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
          val bitmap = pageBitmap
          if (bitmap == null) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(SubscriptionScreenTestTags.LOADING_INDICATOR),
            )
          } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Subscription pass",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag(SubscriptionScreenTestTags.PDF_IMAGE),
            )
          }
        }

        // A scan the scanner declines must not count as an entry. There's no dedicated
        // "Declined" control for that: simply not tapping Accepted already leaves nothing
        // recorded, and the pass stays up for the user to try scanning again.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
          Button(
              onClick = {
                viewModel.onScannerAccepted()
                navigationActions?.goBack()
              },
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
              modifier = Modifier.testTag(SubscriptionScreenTestTags.ACCEPT_BUTTON),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Accepted")
          }
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
