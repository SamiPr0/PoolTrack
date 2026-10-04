package com.github.se.pooltrack.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationMenusTest {

  @get:Rule val composeRule = createComposeRule()

  // --- Tab ---

  @Test
  fun tab_mapsEachTabToItsScreenNameAndTag() {
    assertEquals("Home", Tab.Home.name)
    assertEquals(Screen.Home, Tab.Home.destination)
    assertEquals(NavigationTestTags.HOME_TAB, Tab.Home.testTag)
    assertEquals("Subscriptions", Tab.Subscription.name)
    assertEquals(Screen.Subscription, Tab.Subscription.destination)
    assertEquals(NavigationTestTags.SUBSCRIPTION_TAB, Tab.Subscription.testTag)
    assertEquals("History", Tab.History.name)
    assertEquals(Screen.History, Tab.History.destination)
    assertEquals(NavigationTestTags.HISTORY_TAB, Tab.History.testTag)
  }

  // --- BottomNavigationMenu ---

  @Test
  fun bottomNavigationMenu_showsAllThreeTabsWithLabels() {
    composeRule.setContent { BottomNavigationMenu(selectedTab = Tab.Home, onTabSelected = {}) }

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.SUBSCRIPTION_TAB).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.HISTORY_TAB).assertIsDisplayed()
    composeRule.onNodeWithText("Home").assertIsDisplayed()
    composeRule.onNodeWithText("Subscriptions").assertIsDisplayed()
    composeRule.onNodeWithText("History").assertIsDisplayed()
  }

  @Test
  fun bottomNavigationMenu_marksOnlyTheSelectedTab() {
    composeRule.setContent {
      BottomNavigationMenu(selectedTab = Tab.Subscription, onTabSelected = {})
    }

    composeRule.onNodeWithTag(NavigationTestTags.SUBSCRIPTION_TAB).assertIsSelected()
    composeRule.onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsNotSelected()
    composeRule.onNodeWithTag(NavigationTestTags.HISTORY_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomNavigationMenu_reportsTheClickedTab() {
    val selected = mutableListOf<Tab>()
    composeRule.setContent {
      BottomNavigationMenu(selectedTab = Tab.Home, onTabSelected = { selected += it })
    }

    composeRule.onNodeWithTag(NavigationTestTags.HISTORY_TAB).performClick()
    composeRule.onNodeWithTag(NavigationTestTags.SUBSCRIPTION_TAB).performClick()
    composeRule.onNodeWithTag(NavigationTestTags.HOME_TAB).performClick()

    assertEquals(listOf<Tab>(Tab.History, Tab.Subscription, Tab.Home), selected)
  }

  // --- TopNavigationMenu ---

  @Test
  fun topNavigationMenu_showsTheScreenName() {
    composeRule.setContent { TopNavigationMenu(currentScreen = Screen.History) }

    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("History")
  }

  @Test
  fun topNavigationMenu_hidesBackButton_onTopLevelScreens() {
    composeRule.setContent { TopNavigationMenu(currentScreen = Screen.Home) }

    composeRule.onAllNodesWithTag(NavigationTestTags.GO_BACK_BUTTON).assertCountEquals(0)
  }

  @Test
  fun topNavigationMenu_showsBackButtonAndNotifies_onNonTopLevelScreens() {
    var backClicks = 0
    composeRule.setContent {
      TopNavigationMenu(
          currentScreen = Screen.SubscriptionQuickView,
          onGoBack = { backClicks++ },
      )
    }

    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Subscription")
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed().performClick()

    assertEquals(1, backClicks)
  }

  @Test
  fun topNavigationMenu_rendersTheGivenActions() {
    composeRule.setContent {
      TopNavigationMenu(currentScreen = Screen.Home, actions = { Text("extra action") })
    }

    composeRule.onNodeWithText("extra action").assertIsDisplayed()
  }
}
