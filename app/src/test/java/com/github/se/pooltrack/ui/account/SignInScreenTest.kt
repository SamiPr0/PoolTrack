package com.github.se.pooltrack.ui.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeAuthRepository.Companion.GOOGLE_USER
import com.github.se.pooltrack.utils.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInScreenTest {

  @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
  @get:Rule(order = 1) val composeRule = createComposeRule()

  private fun show(repository: FakeAuthRepository): AccountViewModel {
    val viewModel = AccountViewModel(repository)
    composeRule.setContent { SignInScreen(viewModel = viewModel) }
    return viewModel
  }

  @Test
  fun signInScreen_explainsWhySigningInIsRequired() {
    show(FakeAuthRepository())

    composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    composeRule
        .onNodeWithText("PoolTrack backs up your subscriptions and entries to your Google account.")
        .assertIsDisplayed()
    composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun signInButton_signsTheUserIn_whenGoogleSignInSucceeds() {
    val viewModel = show(FakeAuthRepository())

    composeRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(GOOGLE_USER, viewModel.currentUser.value)
    composeRule.onNodeWithText("Sign-in failed").assertDoesNotExist()
  }

  @Test
  fun signInButton_showsTheErrorInASnackbar_whenGoogleSignInFails() {
    val repository =
        FakeAuthRepository(
            googleSignInResult = Result.failure(IllegalStateException("No Google account found"))
        )
    val viewModel = show(repository)

    composeRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("No Google account found").assertIsDisplayed()
    assertNull(viewModel.currentUser.value)
  }

  @Test
  fun signInButton_showsAGenericSnackbar_whenTheFailureHasNoMessage() {
    val repository = FakeAuthRepository(googleSignInResult = Result.failure(RuntimeException()))
    show(repository)

    composeRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Sign-in failed").assertIsDisplayed()
  }
}
