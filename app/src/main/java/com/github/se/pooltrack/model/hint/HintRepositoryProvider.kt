package com.github.se.pooltrack.model.hint

import android.content.Context

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes. The on-device implementation needs a [Context], so [init] must be called (e.g. from
 * `MainActivity`) before `repository` is first read.
 */
object HintRepositoryProvider {
  lateinit var repository: HintRepository

  /** Creates the on-device repository, unless one was already set (e.g. a fake in a test). */
  fun init(context: Context) {
    if (!::repository.isInitialized) {
      repository = HintRepositoryLocal(context.applicationContext)
    }
  }
}
