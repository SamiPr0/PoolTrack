package com.github.se.pooltrack.ui.account

import androidx.test.core.app.ApplicationProvider
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryProvider
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FirebaseTestApp
import com.github.se.pooltrack.utils.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Covers the default constructor arguments, which come from the provider and the build type. */
@RunWith(RobolectricTestRunner::class)
class AccountViewModelDefaultsTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var originalRepository: AuthRepository

  @Before
  fun setUp() {
    FirebaseTestApp.ensureInitialized(ApplicationProvider.getApplicationContext())
    originalRepository = AuthRepositoryProvider.repository
  }

  @After
  fun tearDown() {
    AuthRepositoryProvider.repository = originalRepository
  }

  @Test
  fun constructor_usesTheProvidedRepository() {
    val repository = FakeAuthRepository()
    AuthRepositoryProvider.repository = repository

    val viewModel = AccountViewModel()

    viewModel.onSignOutClick()
    assertEquals(1, repository.signOutCalls)
  }
}
