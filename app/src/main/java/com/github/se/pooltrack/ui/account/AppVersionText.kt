package com.github.se.pooltrack.ui.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.se.pooltrack.BuildConfig

object AppVersionTextTestTags {
  const val VERSION = "AppVersionText"
}

/**
 * The installed app's version, e.g. "Version 1.2.0", so the user can tell which release they run
 * and whether an update went through.
 */
@Composable
fun AppVersionText(modifier: Modifier = Modifier, version: String = BuildConfig.VERSION_NAME) {
  Text(
      text = "Version $version",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier.testTag(AppVersionTextTestTags.VERSION),
  )
}
