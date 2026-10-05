package com.github.se.pooltrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryProvider
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.subscription.SubscriptionRepositoryProvider
import com.github.se.pooltrack.model.update.UpdateRepositoryProvider
import com.github.se.pooltrack.ui.account.SignInScreenTestTags
import com.github.se.pooltrack.ui.navigation.NavigationTestTags
import com.github.se.pooltrack.ui.update.UpdateDialogTestTags
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeAuthRepository.Companion.GOOGLE_USER
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.FakeUpdateRepository
import com.github.se.pooltrack.utils.FirebaseTestApp
import com.github.se.pooltrack.utils.MainDispatcherRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

  @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
  @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

  private lateinit var originalAuthRepository: AuthRepository

  @Before
  fun setUp() {
    FirebaseTestApp.ensureInitialized(ApplicationProvider.getApplicationContext())
    originalAuthRepository = AuthRepositoryProvider.repository
    EntryRepositoryProvider.repository = FakeEntryRepository()
    SubscriptionRepositoryProvider.repository = FakeSubscriptionRepository()
    UpdateRepositoryProvider.repository = FakeUpdateRepository()
  }

  @After
  fun tearDown() {
    AuthRepositoryProvider.repository = originalAuthRepository
  }

  @Test
  fun onCreate_showsSignIn_whenNobodyIsSignedIn() {
    AuthRepositoryProvider.repository = FakeAuthRepository(initialUser = null)

    ActivityScenario.launch(MainActivity::class.java).use {
      // Debug builds sign in anonymously on start, which is still gated behind real sign-in.
      composeRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
    }
  }

  @Test
  fun onCreate_showsHome_whenSignedInWithGoogle() {
    AuthRepositoryProvider.repository = FakeAuthRepository(initialUser = GOOGLE_USER)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Home")
      composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    }
  }

  @Test
  fun onCreate_offersTheUpdate_whenANewReleaseExists() {
    UpdateRepositoryProvider.repository = FakeUpdateRepository(update = FakeUpdateRepository.UPDATE)
    AuthRepositoryProvider.repository = FakeAuthRepository(initialUser = GOOGLE_USER)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeRule
          .onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE)
          .assertTextEquals("Update available")
    }
  }

  @Test
  fun onCreate_offersTheUpdate_beforeSigningIn() {
    UpdateRepositoryProvider.repository = FakeUpdateRepository(update = FakeUpdateRepository.UPDATE)
    AuthRepositoryProvider.repository = FakeAuthRepository(initialUser = null)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeRule.onNodeWithTag(UpdateDialogTestTags.DIALOG_TITLE).assertIsDisplayed()
    }
  }
}
