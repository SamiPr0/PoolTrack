package com.github.se.pooltrack.model.subscription

import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryProvider
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.model.productionPreferencesDataStore
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import com.github.se.pooltrack.utils.FirebaseTestApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SubscriptionRepositoryProviderTest {

  private var previous: SubscriptionRepository? = null

  private lateinit var previousAuth: AuthRepository
  private val auth = FakeAuthRepository(initialUser = FakeAuthRepository.GOOGLE_USER)

  @Before
  fun setUp() = runTest {
    // init() reads AuthRepositoryProvider, whose default builds the real Firebase repository.
    FirebaseTestApp.ensureInitialized(RuntimeEnvironment.getApplication())
    previousAuth = AuthRepositoryProvider.repository
    AuthRepositoryProvider.repository = auth
    previous = currentRepository()
    clearPreferencesDataStore(RuntimeEnvironment.getApplication(), FILE_CLASS, PROPERTY)
  }

  @After
  fun tearDown() {
    setRepository(previous)
    AuthRepositoryProvider.repository = previousAuth
  }

  // `repository` is a lateinit, so the only way to put the singleton back into its "never
  // initialised" state is through its backing field.
  private fun field() =
      SubscriptionRepositoryProvider::class.java.getDeclaredField("repository").apply {
        isAccessible = true
      }

  private fun currentRepository() =
      field().get(SubscriptionRepositoryProvider) as? SubscriptionRepository

  private fun setRepository(repository: SubscriptionRepository?) {
    field().set(SubscriptionRepositoryProvider, repository)
  }

  @Test
  fun init_createsOnDeviceRepository_whenNoneWasSet() {
    setRepository(null)

    SubscriptionRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertTrue(SubscriptionRepositoryProvider.repository is SubscriptionRepositoryLocal)
  }

  @Test
  fun init_keepsExistingRepository_whenOneWasAlreadySet() {
    val fake = FakeSubscriptionRepository()
    SubscriptionRepositoryProvider.repository = fake

    SubscriptionRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertSame(fake, SubscriptionRepositoryProvider.repository)
  }

  @Test
  fun init_scopesSubscriptionsToTheSignedInAccount() = runTest {
    setRepository(null)
    SubscriptionRepositoryProvider.init(RuntimeEnvironment.getApplication())

    SubscriptionRepositoryProvider.repository.setActiveSubscription("a")

    val prefs =
        productionPreferencesDataStore(RuntimeEnvironment.getApplication(), FILE_CLASS, PROPERTY)
            .data
            .first()
    val uid = FakeAuthRepository.GOOGLE_USER.uid
    assertEquals("a", prefs[stringPreferencesKey("active_subscription_id_$uid")])
  }

  private companion object {
    const val FILE_CLASS =
        "com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocalKt"
    const val PROPERTY = "subscriptionDataStore"
  }
}
