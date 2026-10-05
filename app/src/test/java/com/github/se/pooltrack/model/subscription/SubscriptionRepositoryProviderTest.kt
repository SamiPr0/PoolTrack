package com.github.se.pooltrack.model.subscription

import com.github.se.pooltrack.utils.FakeSubscriptionRepository
import org.junit.After
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

  @Before
  fun setUp() {
    previous = currentRepository()
  }

  @After
  fun tearDown() {
    setRepository(previous)
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
}
