package com.github.se.pooltrack.model.entry

import android.content.Context
import com.github.se.pooltrack.model.auth.AuthRepositoryProvider
import kotlinx.coroutines.flow.map

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes. The on-device implementation needs a [Context], so [init] must be called (e.g. from
 * `MainActivity`) before `repository` is first read. Its data is scoped to the account signed in
 * through [AuthRepositoryProvider].
 */
object EntryRepositoryProvider {
  lateinit var repository: EntryRepository

  /** Creates the on-device repository, unless one was already set (e.g. a fake in a test). */
  fun init(context: Context) {
    if (!::repository.isInitialized) {
      repository =
          EntryRepositoryLocal(
              context.applicationContext,
              currentUid = AuthRepositoryProvider.repository.getCurrentUser().map { it?.uid },
          )
    }
  }
}
