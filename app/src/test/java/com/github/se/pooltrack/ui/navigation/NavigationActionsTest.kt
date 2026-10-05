package com.github.se.pooltrack.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationActionsTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var actions: NavigationActions
  private var openedEntryTimestamp: Long? = null

  @Before
  fun setUp() {
    composeRule.setContent {
      navController = rememberNavController()
      actions = NavigationActions(navController)
      NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { Text("home") }
        composable(Screen.Subscription.route) { Text("subscription") }
        composable(Screen.History.route) { Text("history") }
        composable(Screen.SubscriptionQuickView.route) { Text("quick view") }
        composable(
            Screen.EntryDetails.route,
            arguments = listOf(navArgument(ENTRY_TIMESTAMP_ARG) { type = NavType.LongType }),
        ) { backStackEntry ->
          openedEntryTimestamp = backStackEntry.arguments?.getLong(ENTRY_TIMESTAMP_ARG)
          Text("entry details")
        }
      }
    }
    composeRule.waitForIdle()
  }

  private fun onUi(block: () -> Unit) {
    composeRule.runOnUiThread(block)
    composeRule.waitForIdle()
  }

  @Test
  fun screen_exposesRoutesNamesAndTopLevelFlags() {
    assertEquals(true, Screen.Home.isTopLevelDestination)
    assertEquals(true, Screen.History.isTopLevelDestination)
    assertEquals(true, Screen.Subscription.isTopLevelDestination)
    assertEquals("home", Screen.Home.route)
    assertEquals("history", Screen.History.route)
    assertEquals("subscription", Screen.Subscription.route)
    assertEquals("Home", Screen.Home.name)
    assertEquals("History", Screen.History.name)
    assertEquals("Subscriptions", Screen.Subscription.name)
    assertEquals("Subscription", Screen.SubscriptionQuickView.name)
    assertEquals("subscription_quick_view", Screen.SubscriptionQuickView.route)
    assertEquals(false, Screen.SubscriptionQuickView.isTopLevelDestination)
  }

  @Test
  fun currentRoute_isTheStartDestination_atLaunch() {
    assertEquals("home", actions.currentRoute())
  }

  @Test
  fun currentRoute_isEmpty_whenThereIsNoCurrentDestination() {
    val controller = mockk<NavHostController>()
    every { controller.currentDestination } returns null

    assertEquals("", NavigationActions(controller).currentRoute())
  }

  @Test
  fun navigateTo_opensTheTopLevelScreen_fromAnotherScreen() {
    onUi { actions.navigateTo(Screen.History) }

    assertEquals("history", actions.currentRoute())
  }

  @Test
  fun navigateTo_doesNotStackTheSameTopLevelScreen_whenAlreadyThere() {
    onUi { actions.navigateTo(Screen.History) }
    val before = navController.currentBackStack.value.size

    onUi { actions.navigateTo(Screen.History) }

    assertEquals("history", actions.currentRoute())
    assertEquals(before, navController.currentBackStack.value.size)
  }

  @Test
  fun navigateTo_doesNothing_whenAlreadyOnTheTopLevelScreen() {
    val controller = mockk<NavHostController>(relaxed = true)
    val destination = mockk<NavDestination>()
    every { destination.route } returns "history"
    every { controller.currentDestination } returns destination

    NavigationActions(controller).navigateTo(Screen.History)

    verify(exactly = 0) { controller.navigate(any<String>(), any<NavOptions>(), any()) }
  }

  @Test
  fun navigateTo_resetsTheBackStack_whenSwitchingBetweenTopLevelScreens() {
    onUi { actions.navigateTo(Screen.History) }
    onUi { actions.navigateTo(Screen.Home) }

    assertEquals("home", actions.currentRoute())
    // Home is re-created on top of nothing: going back leaves no previous screen.
    assertNull(navController.previousBackStackEntry)
  }

  @Test
  fun navigateTo_stacksANonTopLevelScreen_onTopOfTheCurrentOne() {
    onUi { actions.navigateTo(Screen.SubscriptionQuickView) }

    assertEquals("subscription_quick_view", actions.currentRoute())
    assertEquals("home", navController.previousBackStackEntry?.destination?.route)
  }

  @Test
  fun navigateTo_opensANonTopLevelScreenAgain_evenIfAlreadyThere() {
    onUi { actions.navigateTo(Screen.SubscriptionQuickView) }
    val before = navController.currentBackStack.value.size

    onUi { actions.navigateTo(Screen.SubscriptionQuickView) }

    assertEquals(before + 1, navController.currentBackStack.value.size)
  }

  @Test
  fun screen_entryDetails_isANonTopLevelScreenWithATimestampArgument() {
    assertEquals("entry_details/{entryTimestamp}", Screen.EntryDetails.route)
    assertEquals("entry_details/42", Screen.EntryDetails.routeFor(42L))
    assertEquals("Swim", Screen.EntryDetails.name)
    assertEquals(false, Screen.EntryDetails.isTopLevelDestination)
  }

  @Test
  fun navigateToEntryDetails_opensThatEntry_onTopOfTheCurrentScreen() {
    onUi { actions.navigateTo(Screen.History) }

    onUi { actions.navigateToEntryDetails(1_234L) }

    assertEquals(Screen.EntryDetails.route, actions.currentRoute())
    assertEquals(1_234L, openedEntryTimestamp)
    assertEquals("history", navController.previousBackStackEntry?.destination?.route)
  }

  @Test
  fun goBack_returnsToThePreviousScreen() {
    onUi { actions.navigateTo(Screen.SubscriptionQuickView) }

    onUi { actions.goBack() }

    assertEquals("home", actions.currentRoute())
  }

  @Test
  fun goBack_popsTheBackStack_onTheNavController() {
    val controller = mockk<NavHostController>(relaxed = true)

    NavigationActions(controller).goBack()

    verify(exactly = 1) { controller.popBackStack() }
  }
}
