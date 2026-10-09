package com.github.se.pooltrack.model.hint

import com.github.se.pooltrack.utils.FakeHintRepository
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HintRepositoryProviderTest {

  private var previous: HintRepository? = null

  @Before
  fun setUp() {
    previous = field().get(HintRepositoryProvider) as? HintRepository
  }

  @After
  fun tearDown() {
    field().set(HintRepositoryProvider, previous)
  }

  // `repository` is a lateinit, so the only way to put the singleton back into its "never
  // initialised" state is through its backing field.
  private fun field() =
      HintRepositoryProvider::class.java.getDeclaredField("repository").apply {
        isAccessible = true
      }

  @Test
  fun init_createsOnDeviceRepository_whenNoneWasSet() {
    field().set(HintRepositoryProvider, null)

    HintRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertTrue(HintRepositoryProvider.repository is HintRepositoryLocal)
  }

  @Test
  fun init_keepsExistingRepository_whenOneWasAlreadySet() {
    val fake = FakeHintRepository()
    HintRepositoryProvider.repository = fake

    HintRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertSame(fake, HintRepositoryProvider.repository)
  }
}
