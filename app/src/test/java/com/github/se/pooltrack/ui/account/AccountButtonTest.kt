package com.github.se.pooltrack.ui.account

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.pooltrack.BuildConfig
import com.github.se.pooltrack.model.auth.AuthUser
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeAuthRepository.Companion.ANONYMOUS_USER
import com.github.se.pooltrack.utils.FakeAuthRepository.Companion.GOOGLE_USER
import com.github.se.pooltrack.utils.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountButtonTest {

  @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
  @get:Rule(order = 1) val composeRule = createComposeRule()

  private val errors = mutableListOf<String>()

  private fun show(repository: FakeAuthRepository): AccountViewModel {
    val viewModel = AccountViewModel(repository)
    composeRule.setContent {
      Row { AccountButton(onError = { errors += it }, viewModel = viewModel) }
    }
    return viewModel
  }

  @Test
  fun accountButton_describesItselfAsAccount_whenSignedInWithGoogle() {
    show(FakeAuthRepository(initialUser = GOOGLE_USER))

    composeRule.onNodeWithContentDescription("Account").assertIsDisplayed()
  }

  @Test
  fun accountButton_offersSignIn_whenSignedOut() {
    show(FakeAuthRepository())

    composeRule.onNodeWithContentDescription("Sign in to back up your data").assertIsDisplayed()
  }

  @Test
  fun accountButton_offersSignIn_whenAnonymous() {
    show(FakeAuthRepository(initialUser = ANONYMOUS_USER))

    composeRule.onNodeWithContentDescription("Sign in to back up your data").assertIsDisplayed()
  }

  @Test
  fun accountButton_opensTheDialog_whenClickedWhileSignedIn() {
    show(FakeAuthRepository(initialUser = GOOGLE_USER))

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_DIALOG).assertIsDisplayed()
    composeRule.onNodeWithText("Signed in").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Signed in as Swimmer. Subscriptions and entries are backed up to the cloud."
        )
        .assertIsDisplayed()
    composeRule.onNodeWithText("Sign out").assertIsDisplayed()
    composeRule.onNodeWithText("Close").assertIsDisplayed()
  }

  @Test
  fun accountButton_dialogShowsTheInstalledVersion() {
    show(FakeAuthRepository(initialUser = GOOGLE_USER))

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule
        .onNodeWithTag(AppVersionTextTestTags.VERSION)
        .assertTextEquals("Version ${BuildConfig.VERSION_NAME}")
  }

  @Test
  fun accountButton_dialogFallsBackToEmail_whenThereIsNoDisplayName() {
    val user = AuthUser("uid", displayName = null, email = "me@pool.io", isAnonymous = false)
    show(FakeAuthRepository(initialUser = user))

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule
        .onNodeWithText(
            "Signed in as me@pool.io. Subscriptions and entries are backed up to the cloud."
        )
        .assertIsDisplayed()
  }

  @Test
  fun accountButton_dialogFallsBackToGenericName_whenThereIsNeitherNameNorEmail() {
    val user = AuthUser("uid", displayName = null, email = null, isAnonymous = false)
    show(FakeAuthRepository(initialUser = user))

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule
        .onNodeWithText(
            "Signed in as your Google account. Subscriptions and entries are backed up to the cloud."
        )
        .assertIsDisplayed()
  }

  @Test
  fun accountButton_closeDismissesTheDialog_withoutSigningOut() {
    val repository = FakeAuthRepository(initialUser = GOOGLE_USER)
    show(repository)
    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule.onNodeWithText("Close").performClick()

    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_DIALOG).assertDoesNotExist()
    assertEquals(0, repository.signOutCalls)
  }

  @Test
  fun accountButton_signOutSignsOutAndClosesTheDialog() {
    val repository = FakeAuthRepository(initialUser = GOOGLE_USER)
    val viewModel = show(repository)
    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()

    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_BUTTON).performClick()

    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_DIALOG).assertDoesNotExist()
    assertEquals(1, repository.signOutCalls)
    assertNull(viewModel.currentUser.value)
    composeRule.onNodeWithContentDescription("Sign in to back up your data").assertIsDisplayed()
  }

  @Test
  fun accountButton_startsGoogleSignIn_whenClickedWhileSignedOut() {
    val repository = FakeAuthRepository()
    val viewModel = show(repository)

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(GOOGLE_USER, viewModel.currentUser.value)
    assertEquals(emptyList<String>(), errors)
    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_DIALOG).assertDoesNotExist()
    composeRule.onNodeWithContentDescription("Account").assertIsDisplayed()
  }

  @Test
  fun accountButton_reportsTheError_whenSignInFailsWhileSignedOut() {
    val repository =
        FakeAuthRepository(googleSignInResult = Result.failure(IllegalStateException("Cancelled")))
    val viewModel = show(repository)

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(listOf("Cancelled"), errors)
    assertNull(viewModel.currentUser.value)
  }

  @Test
  fun accountButton_startsGoogleSignIn_whenClickedWhileAnonymous() {
    val repository = FakeAuthRepository(initialUser = ANONYMOUS_USER)
    val viewModel = show(repository)

    composeRule.onNodeWithTag(AccountButtonTestTags.BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(GOOGLE_USER, viewModel.currentUser.value)
    composeRule.onNodeWithTag(AccountButtonTestTags.SIGN_OUT_DIALOG).assertDoesNotExist()
    composeRule.onNode(hasContentDescription("Account")).assertIsDisplayed()
  }
}
