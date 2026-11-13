package com.github.se.pooltrack.ui.theme

import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolMotionTest {

  private val resolver
    get() = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

  private fun setAnimatorScale(scale: Float) {
    Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
  }

  @Test
  fun animationsEnabled_followsTheSystemAnimatorScale() {
    setAnimatorScale(1f)
    assertTrue(animationsEnabled(resolver))

    setAnimatorScale(0f)
    assertFalse(animationsEnabled(resolver))
  }
}
