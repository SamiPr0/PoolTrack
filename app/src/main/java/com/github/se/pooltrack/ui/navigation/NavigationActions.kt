package com.github.se.pooltrack.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false,
) {

  object Home :
      Screen(
          route = "home",
          name = "Home",
          isTopLevelDestination = true,
      )

  object History :
      Screen(
          route = "history",
          name = "History",
          isTopLevelDestination = true,
      )

  /**
   * Manages subscriptions: add, switch which is active, or delete. See also
   * [SubscriptionQuickView].
   */
  object Subscription :
      Screen(
          route = "subscription",
          name = "Subscriptions",
          isTopLevelDestination = true,
      )

  /**
   * The focused "just show my pass" flow opened from Home's FAB: view + accept + back, with none of
   * [Subscription]'s management actions.
   */
  object SubscriptionQuickView :
      Screen(
          route = "subscription_quick_view",
          name = "Subscription",
      )

  /** Everything about one confirmed entry, opened by tapping it in [History]. */
  object EntryDetails :
      Screen(
          route = "entry_details/{$ENTRY_TIMESTAMP_ARG}",
          name = "Swim",
      ) {
    /** The route of the details of the entry confirmed at [timestampEpochMilli]. */
    fun routeFor(timestampEpochMilli: Long): String = "entry_details/$timestampEpochMilli"
  }
}

/** The [Screen.EntryDetails] route argument: the entry's `timestampEpochMilli`. */
const val ENTRY_TIMESTAMP_ARG = "entryTimestamp"

open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) {
      return
    }
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(screen.route) { inclusive = true }
      }
      restoreState = true
    }
  }

  /**
   * Opens the details of the entry confirmed at [timestampEpochMilli], on top of the current
   * screen.
   */
  open fun navigateToEntryDetails(timestampEpochMilli: Long) {
    navController.navigate(Screen.EntryDetails.routeFor(timestampEpochMilli))
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
