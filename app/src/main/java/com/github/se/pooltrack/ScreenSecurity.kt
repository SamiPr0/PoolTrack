package com.github.se.pooltrack

import android.view.Window
import android.view.WindowManager

/**
 * Marks this window as secure, or not. A secure window can't be screenshotted, screen-recorded or
 * cast, and shows blank in the recent-apps switcher, so the pass and the pool history never leave
 * the screen. Compose dialogs inherit the flag from the window they're opened over.
 */
fun Window.applyScreenSecurity(secure: Boolean) {
  if (secure) {
    addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  } else {
    clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
  }
}
