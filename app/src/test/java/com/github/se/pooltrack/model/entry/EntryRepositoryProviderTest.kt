package com.github.se.pooltrack.model.entry

import com.github.se.pooltrack.model.auth.AuthRepository
import com.github.se.pooltrack.model.auth.AuthRepositoryProvider
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.utils.FakeAuthRepository
import com.github.se.pooltrack.utils.FakeEntryRepository
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
class EntryRepositoryProviderTest {

  private var previous: EntryRepository? = null

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

  @Test
  fun init_scopesEntriesToTheSignedInAccount() = runTest {
    setRepository(null)
    EntryRepositoryProvider.init(RuntimeEnvironment.getApplication())

    EntryRepositoryProvider.repository.addEntry(Entry(timestampEpochMilli = 1L))
    assertEquals(1, EntryRepositoryProvider.repository.getEntries().first().size)

    auth.signOut()

    assertEquals(0, EntryRepositoryProvider.repository.getEntries().first().size)
  }

  private companion object {
    const val FILE_CLASS = "com.github.se.pooltrack.model.entry.EntryRepositoryLocalKt"
    const val PROPERTY = "entryDataStore"
  }
}
