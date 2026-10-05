package com.github.se.pooltrack.model

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

/**
 * The DataStore that the production code declares as a private top-level delegate
 * (`preferencesDataStore`) named [propertyName] in [fileClassName], e.g.
 * `SubscriptionRepositoryLocalKt`.
 */
@Suppress("UNCHECKED_CAST")
internal fun productionPreferencesDataStore(
    context: Context,
    fileClassName: String,
    propertyName: String,
): DataStore<Preferences> {
  val getter =
      Class.forName(fileClassName)
          .getDeclaredMethod(
              "get${propertyName.replaceFirstChar { it.uppercase() }}",
              Context::class.java,
          )
  getter.isAccessible = true
  return getter.invoke(null, context.applicationContext) as DataStore<Preferences>
}

/**
 * Empties that DataStore. The delegate caches one DataStore per process, so it would otherwise leak
 * state from one test to the next even though Robolectric gives every test a fresh files directory.
 */
internal suspend fun clearPreferencesDataStore(
    context: Context,
    fileClassName: String,
    propertyName: String,
) {
  productionPreferencesDataStore(context, fileClassName, propertyName).edit { it.clear() }
}

/** A [Context] that behaves like [base] except that it hands out [resolver]. */
internal class ResolverContext(base: Context, private val resolver: ContentResolver) :
    ContextWrapper(base) {
  override fun getContentResolver(): ContentResolver = resolver
}
