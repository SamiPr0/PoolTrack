package com.github.se.pooltrack.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A [Scaffold] laid on the pool floor ([poolTiles]): the bars and the content of a screen sit on
 * the tiles rather than on a plain page.
 */
@Composable
fun PoolScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
      modifier = Modifier.poolTiles(MaterialTheme.colorScheme),
      topBar = topBar,
      bottomBar = bottomBar,
      snackbarHost = snackbarHost,
      floatingActionButton = floatingActionButton,
      containerColor = Color.Transparent,
      content = content,
  )
}
