package com.github.se.pooltrack

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File
import java.lang.reflect.Modifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Guards the security-relevant settings of the merged manifest. */
@RunWith(RobolectricTestRunner::class)
class ManifestSecurityTest {

  private val context = RuntimeEnvironment.getApplication()

  // FileProvider caches each authority's folder mapping in a static map for the whole JVM, while
  // Robolectric gives every test a fresh data folder. A mapping cached by an earlier test points at
  // that test's folder and makes later FileProvider calls fail, so it's cleared on both sides.
  @Before
  @After
  fun clearFileProviderCache() {
    val caches =
        FileProvider::class.java.declaredFields.filter {
          Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type)
        }
    check(caches.isNotEmpty()) { "FileProvider no longer has a static cache to clear" }
    caches.forEach { field ->
      field.isAccessible = true
      (field.get(null) as MutableMap<*, *>).clear()
    }
  }

  @Test
  fun application_disallowsCloudBackup() {
    val flags = context.applicationInfo.flags

    assertEquals(0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
  }

  @Test
  fun application_hasNoBackupAgent() {
    assertEquals(null, context.applicationInfo.backupAgentName)
  }

  // Debug builds also export ui-test-manifest's test host activity, so only the app's own
  // activities are checked.
  @Test
  fun onlyTheLauncherActivity_isExported() {
    val activities =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            .activities
            .orEmpty()
    val exported =
        activities.filter { it.exported && it.name.startsWith(context.packageName) }.map { it.name }

    assertEquals(listOf(MainActivity::class.java.name), exported)
    assertTrue(activities.isNotEmpty())
  }

  // Includes components merged in from libraries, which can declare their own.
  @Test
  fun everyExportedService_providerAndReceiver_isPermissionProtected() {
    val info =
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_RECEIVERS,
        )
    val unprotected =
        info.services.orEmpty().filter { it.exported && it.permission == null } +
            info.providers.orEmpty().filter {
              it.exported && it.readPermission == null && it.writePermission == null
            } +
            info.receivers.orEmpty().filter { it.exported && it.permission == null }

    assertEquals(emptyList<String>(), unprotected.map(ComponentInfo::name))
  }

  @Test
  fun fileProvider_sharesDownloadedUpdates() {
    assumeNotWindows()
    val apk = File(context.cacheDir, "updates/PoolTrack-1.2.0.apk")

    val uri = FileProvider.getUriForFile(context, AUTHORITY, apk)

    assertEquals("content", uri.scheme)
  }

  // The provider hands files to the system installer, so it must never be able to expose the
  // app's stored subscriptions and entries, or anything else outside the updates folder.
  @Test
  fun fileProvider_refusesFilesOutsideTheUpdatesFolder() {
    assumeNotWindows()
    val outside =
        listOf(
            File(context.filesDir, "datastore/entry_prefs.preferences_pb"),
            File(context.filesDir, "datastore/subscription_prefs.preferences_pb"),
            File(context.cacheDir, "other.apk"),
            File(context.cacheDir, "updates/../other.apk"),
        )

    for (file in outside) {
      assertThrows(IllegalArgumentException::class.java) {
        FileProvider.getUriForFile(context, AUTHORITY, file)
      }
    }
  }

  // FileProvider only matches a file to its root when the next path character is '/', so on Windows
  // it refuses every file and these tests would pass or fail for the wrong reason. CI runs on
  // Linux.
  private fun assumeNotWindows() {
    assumeFalse(System.getProperty("os.name").startsWith("Windows"))
  }

  private companion object {
    const val AUTHORITY = "com.github.se.pooltrack.fileprovider"
  }
}
