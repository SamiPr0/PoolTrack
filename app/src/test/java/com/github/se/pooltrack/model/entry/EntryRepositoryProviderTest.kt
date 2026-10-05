package com.github.se.pooltrack.model.entry

import com.github.se.pooltrack.utils.FakeEntryRepository
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EntryRepositoryProviderTest {

  private var previous: EntryRepository? = null

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
      EntryRepositoryProvider::class.java.getDeclaredField("repository").apply {
        isAccessible = true
      }

  private fun currentRepository() = field().get(EntryRepositoryProvider) as? EntryRepository

  private fun setRepository(repository: EntryRepository?) {
    field().set(EntryRepositoryProvider, repository)
  }

  @Test
  fun init_createsOnDeviceRepository_whenNoneWasSet() {
    setRepository(null)

    EntryRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertTrue(EntryRepositoryProvider.repository is EntryRepositoryLocal)
  }

  @Test
  fun init_keepsExistingRepository_whenOneWasAlreadySet() {
    val fake = FakeEntryRepository()
    EntryRepositoryProvider.repository = fake

    EntryRepositoryProvider.init(RuntimeEnvironment.getApplication())

    assertSame(fake, EntryRepositoryProvider.repository)
  }
}
