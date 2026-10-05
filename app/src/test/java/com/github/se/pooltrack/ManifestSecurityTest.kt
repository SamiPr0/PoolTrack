package com.github.se.pooltrack

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Guards the security-relevant settings of the merged manifest. */
@RunWith(RobolectricTestRunner::class)
class ManifestSecurityTest {

  private val context = RuntimeEnvironment.getApplication()

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
}
