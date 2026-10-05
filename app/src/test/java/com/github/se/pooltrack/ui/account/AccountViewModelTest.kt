package com.github.se.pooltrack.ui.account

import android.content.Context
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeAuthRepository.Companion.GOOGLE_USER
import com.github.se.pooltrack.utils.MainDispatcherRule
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val context = mockk<Context>()

  @Test
  fun init_staysSignedOut_whenNobodyIsSignedIn() {
    val viewModel = AccountViewModel(FakeAuthRepository())

    assertNull(viewModel.currentUser.value)
  }

  @Test
  fun currentUser_isTheSignedInUser_whenAlreadySignedIn() {
    val viewModel =
        AccountViewModel(
            FakeAuthRepository(initialUser = GOOGLE_USER),
        )

    assertEquals(GOOGLE_USER, viewModel.currentUser.value)
  }

  @Test
  fun onSignInClick_signsInWithoutError_whenGoogleSignInSucceeds() {
    val viewModel = AccountViewModel(FakeAuthRepository())
    val errors = mutableListOf<String>()

    viewModel.onSignInClick(context) { errors += it }

    assertEquals(GOOGLE_USER, viewModel.currentUser.value)
    assertEquals(emptyList<String>(), errors)
  }

  @Test
  fun onSignInClick_reportsTheFailureMessage_whenGoogleSignInFails() {
    val repository =
        FakeAuthRepository(googleSignInResult = Result.failure(IllegalStateException("No account")))
    val viewModel = AccountViewModel(repository)
    val errors = mutableListOf<String>()

    viewModel.onSignInClick(context) { errors += it }

    assertEquals(listOf("No account"), errors)
    assertNull(viewModel.currentUser.value)
  }

  @Test
  fun onSignInClick_reportsAGenericMessage_whenFailureHasNoMessage() {
    val repository = FakeAuthRepository(googleSignInResult = Result.failure(RuntimeException()))
    val viewModel = AccountViewModel(repository)
    val errors = mutableListOf<String>()

    viewModel.onSignInClick(context) { errors += it }

    assertEquals(listOf("Sign-in failed"), errors)
  }

  @Test
  fun onSignOutClick_signsTheUserOut() {
    val repository = FakeAuthRepository(initialUser = GOOGLE_USER)
    val viewModel = AccountViewModel(repository)

    viewModel.onSignOutClick()

    assertEquals(1, repository.signOutCalls)
    assertNull(viewModel.currentUser.value)
  }
}
