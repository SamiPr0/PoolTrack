package com.github.se.pooltrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.se.pooltrack.ui.home.HomeScreen
import com.github.se.pooltrack.ui.navigation.Screen
import com.github.se.pooltrack.ui.theme.PoolTrackTheme

private val startDestination = Screen.Home.route

/** `MainActivity` is the entry point of the application. */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent { PoolTrackTheme { Surface(modifier = Modifier.fillMaxSize()) { PoolTrackApp() } } }
  }
}

/**
 * `PoolTrackApp` is the main composable function that sets up the whole app UI. It initializes
 * the navigation controller and defines the navigation graph.
 */
@Composable
fun PoolTrackApp() {
  val navController: NavHostController = rememberNavController()

  NavHost(navController = navController, startDestination = startDestination) {
    composable(Screen.Home.route) { HomeScreen() }
  }
}
