package com.github.se.pooltrack.model.subscription

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.model.productionPreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PassZoomRepositoryLocalTest {

  private val zoom = SavedPassZoom(scale = 3f, offsetXFraction = -0.25f, offsetYFraction = 0.5f)

  private lateinit var appContext: Context
  private lateinit var repository: PassZoomRepositoryLocal

  @Before
  fun setUp() = runTest {
    appContext = RuntimeEnvironment.getApplication()
    clearPreferencesDataStore(appContext, FILE_CLASS, PROPERTY)
    repository = PassZoomRepositoryLocal(appContext)
  }

  @Test
  fun load_isNull_whenNothingWasSaved() = runTest { assertNull(repository.load("content://a")) }

  @Test
  fun load_returnsWhatWasSaved() = runTest {
    repository.save("content://a", zoom)

    assertEquals(zoom, repository.load("content://a"))
  }

  @Test
  fun load_survivesANewRepositoryInstance() = runTest {
    repository.save("content://a", zoom)

    assertEquals(zoom, PassZoomRepositoryLocal(appContext).load("content://a"))
  }

  @Test
  fun load_isNull_forAnotherPass() = runTest {
    repository.save("content://a", zoom)

    assertNull(repository.load("content://b"))
  }

  @Test
  fun save_replacesTheZoomSavedBefore() = runTest {
    val newer = SavedPassZoom(2f, 0f, 0f)
    repository.save("content://a", zoom)
    repository.save("content://b", newer)

    assertNull(repository.load("content://a"))
    assertEquals(newer, repository.load("content://b"))
  }

  @Test
  fun load_isNull_whenStoredZoomIsIncomplete() = runTest {
    productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).edit {
      it[stringPreferencesKey("pass_uri")] = "content://a"
      it[floatPreferencesKey("scale")] = 2f
    }

    assertNull(repository.load("content://a"))
  }

  @Test
  fun load_isNull_whenStoredZoomIsNotFinite() = runTest {
    productionPreferencesDataStore(appContext, FILE_CLASS, PROPERTY).edit {
      it[stringPreferencesKey("pass_uri")] = "content://a"
      it[floatPreferencesKey("scale")] = Float.NaN
      it[floatPreferencesKey("offset_x_fraction")] = 0f
      it[floatPreferencesKey("offset_y_fraction")] = 0f
    }

    assertNull(repository.load("content://a"))
  }

  private companion object {
    const val FILE_CLASS = "com.github.se.pooltrack.model.subscription.PassZoomRepositoryLocalKt"
    const val PROPERTY = "passZoomDataStore"
  }
}
