package com.github.se.pooltrack.model.update

import android.content.Context

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes. The GitHub implementation needs a [Context], so [init] must be called (e.g. from
 * `MainActivity`) before `repository` is first read.
 */
object UpdateRepositoryProvider {
  lateinit var repository: UpdateRepository

  /** Creates the GitHub repository, unless one was already set (e.g. a fake in a test). */
  fun init(context: Context) {
    if (!::repository.isInitialized) {
      repository = UpdateRepositoryGithub(context)
    }
  }
}
