package com.github.se.pooltrack.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Registers a Firebase app with dummy options so that `AuthRepositoryProvider`, whose initializer
 * builds the real `AuthRepositoryFirebase`, can be loaded on the JVM. Nothing talks to a backend:
 * tests replace `AuthRepositoryProvider.repository` with a fake right after.
 */
object FirebaseTestApp {

  fun ensureInitialized(context: Context) {
    if (FirebaseApp.getApps(context).isNotEmpty()) return
    val options =
        FirebaseOptions.Builder()
            .setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("test-api-key")
            .setProjectId("pooltrack-test")
            .build()
    FirebaseApp.initializeApp(context, options)
  }
}
