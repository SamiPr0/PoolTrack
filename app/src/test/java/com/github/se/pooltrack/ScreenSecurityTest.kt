package com.github.se.pooltrack

import android.app.Activity
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenSecurityTest {

  private val window = Robolectric.buildActivity(Activity::class.java).setup().get().window

  private val isSecure: Boolean
    get() = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

  @Test
  fun applyScreenSecurity_setsSecureFlag_whenSecure() {
    window.applyScreenSecurity(secure = true)

    assertEquals(true, isSecure)
  }

  @Test
  fun applyScreenSecurity_clearsSecureFlag_whenNotSecure() {
    window.applyScreenSecurity(secure = true)

    window.applyScreenSecurity(secure = false)

    assertEquals(false, isSecure)
  }
}
