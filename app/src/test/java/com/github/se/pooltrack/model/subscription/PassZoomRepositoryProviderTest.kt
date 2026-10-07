package com.github.se.pooltrack.model.subscription

import com.github.se.pooltrack.utils.FakePassZoomRepository
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PassZoomRepositoryProviderTest {

  private var previous: PassZoomRepository? = null

  @Before
  fun setUp() {
    previous = field().get(PassZoomRepositoryProvider) as? PassZoomRepository
  }

  @After
  fun tearDown() {
    field().set(PassZoomRepositoryProvider, previous)
  }

  // `repository` is a lateinit, so the only way to put the singleton back into its "never
  // initialised" state is through its backing field.
  private fun field() =
      PassZoomRepositoryProvider::class.java.getDeclaredField("repository").apply {
        isAccessible = true
      }

  @Test
  fun init_createsOnDeviceRepository_whenNoneWasSet() {
    field().set(PassZoomRepositoryProvider, null)

    PassZoomRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertTrue(PassZoomRepositoryProvider.repository is PassZoomRepositoryLocal)
  }

  @Test
  fun init_keepsExistingRepository_whenOneWasAlreadySet() {
    val fake = FakePassZoomRepository()
    PassZoomRepositoryProvider.repository = fake

    PassZoomRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertSame(fake, PassZoomRepositoryProvider.repository)
  }
}
