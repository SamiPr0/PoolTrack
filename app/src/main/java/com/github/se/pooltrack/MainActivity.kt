package com.github.se.pooltrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.model.swim.isSwimPending
import com.github.se.pooltrack.model.update.UpdateRepositoryProvider
import com.github.se.pooltrack.swim.SwimTrackingService
import com.github.se.pooltrack.ui.account.AccountViewModel
import com.github.se.pooltrack.ui.account.SignInScreen
import com.github.se.pooltrack.ui.history.HistoryScreen
import com.github.se.pooltrack.ui.home.HomeScreen
import com.github.se.pooltrack.ui.navigation.NavigationActions
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.subscription.SubscriptionQuickViewScreen
import com.github.se.pooltrack.ui.subscription.SubscriptionScreen
import com.github.se.pooltrack.ui.theme.PoolTrackTheme
import com.github.se.pooltrack.ui.update.UpdateDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val startDestination = Screen.Home.route

/** `MainActivity` is the entry point of the application. */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Debug builds stay screenshot-able, for design reviews and bug reports.
    window.applyScreenSecurity(secure = !BuildConfig.DEBUG)
    EntryRepositoryProvider.init(this)
    SubscriptionRepositoryProvider.init(this)
    UpdateRepositoryProvider.init(this)
    requestNotificationPermissionIfNeeded()
    startSwimTrackingWhileASwimIsPending()

    setContent { PoolTrackTheme { Surface(modifier = Modifier.fillMaxSize()) { PoolTrackApp() } } }
  }

  // Needed to tell the user how long they swam; without it the swim is tracked but silent.
  private fun requestNotificationPermissionIfNeeded() {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
  }

  // Foreground services may only be started while the app is visible, i.e. right after the
  // user confirmed the entry here, so this watches the entries while the activity is started.
  private fun startSwimTrackingWhileASwimIsPending() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        EntryRepositoryProvider.repository
            .getEntries()
            .map { isSwimPending(it.firstOrNull()) }
            .distinctUntilChanged()
            .collect { pending -> if (pending) SwimTrackingService.start(this@MainActivity) }
      }
    }
  }
}

/**
 * `PoolTrackApp` is the main composable function that sets up the whole app UI. Gated behind Google
 * sign-in - [SignInScreen] is shown instead until a real (non-anonymous) account is signed in -
 * since subscriptions and entries are only meaningful once backed up to one. [UpdateDialog] sits
 * above both, so a new release is offered even before signing in.
 */
@Composable
fun PoolTrackApp(accountViewModel: AccountViewModel = viewModel()) {
  UpdateDialog()

  val currentUser by accountViewModel.currentUser.collectAsState()
  val user = currentUser

  if (user == null || user.isAnonymous) {
    SignInScreen(viewModel = accountViewModel)
    return
  }

  val navController: NavHostController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  NavHost(navController = navController, startDestination = startDestination) {
    composable(Screen.Home.route) { HomeScreen(navigationActions = navigationActions) }
    composable(Screen.Subscription.route) {
      SubscriptionScreen(navigationActions = navigationActions)
    }
    composable(Screen.SubscriptionQuickView.route) {
      SubscriptionQuickViewScreen(navigationActions = navigationActions)
    }
    composable(Screen.History.route) { HistoryScreen(navigationActions = navigationActions) }
  }
}
