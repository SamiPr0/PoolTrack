package com.github.se.pooltrack.model.hint

import android.content.Context
import com.github.se.pooltrack.model.clearPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HintRepositoryLocalTest {

  private lateinit var appContext: Context
  private lateinit var repository: HintRepositoryLocal

  @Before
  fun setUp() = runTest {
    appContext = RuntimeEnvironment.getApplication()
    clearPreferencesDataStore(appContext, FILE_CLASS, PROPERTY)
    repository = HintRepositoryLocal(appContext)
  }

  @Test
  fun isDismissed_isFalse_beforeTheHintWasDismissed() = runTest {
    assertFalse(repository.isDismissed(Hint.SwipeToDelete).first())
  }

  @Test
  fun isDismissed_isTrue_afterDismissing() = runTest {
    repository.dismiss(Hint.SwipeToDelete)

    assertTrue(repository.isDismissed(Hint.SwipeToDelete).first())
  }

  @Test
  fun isDismissed_survivesANewRepositoryInstance() = runTest {
    repository.dismiss(Hint.SwipeToDelete)

    assertTrue(HintRepositoryLocal(appContext).isDismissed(Hint.SwipeToDelete).first())
  }

  @Test
  fun dismiss_twice_changesNothing() = runTest {
    repository.dismiss(Hint.SwipeToDelete)
    repository.dismiss(Hint.SwipeToDelete)

    assertTrue(repository.isDismissed(Hint.SwipeToDelete).first())
  }

  private companion object {
    const val FILE_CLASS = "com.github.se.pooltrack.model.hint.HintRepositoryLocalKt"
    const val PROPERTY = "hintDataStore"
  }
}
