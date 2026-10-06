package com.github.se.pooltrack.utils

import com.github.se.pooltrack.model.update.AppUpdate
import com.github.se.pooltrack.model.update.DownloadStatus
import com.github.se.pooltrack.model.update.InstallResult
import com.github.se.pooltrack.model.update.UpdateRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [UpdateRepository] for unit tests: it reports [update] as the latest release, downloads whatever
 * [downloadFlow] emits and answers installs with [installResult], recording what it was asked.
 */
class FakeUpdateRepository(
    var update: AppUpdate? = null,
    var downloadFlow: Flow<DownloadStatus> = flowOf(DownloadStatus.Finished(APK)),
    var installResult: InstallResult = InstallResult.STARTED,
    var canInstallResult: Boolean = true,
) : UpdateRepository {

  /** The `currentVersion` passed to the last [checkForUpdate], or `null` if never called. */
  var checkedVersion: String? = null
    private set

  /** The updates [download] was called for, in order. */
  val downloadedUpdates = mutableListOf<AppUpdate>()

  /** The APKs [install] was called with, in order. */
  val installedApks = mutableListOf<File>()

  /** How many times [checkForUpdate] was called. */
  var checkCount = 0
    private set

  override suspend fun checkForUpdate(currentVersion: String): AppUpdate? {
    checkCount++
    checkedVersion = currentVersion
    return update
  }

  override fun download(update: AppUpdate): Flow<DownloadStatus> {
    downloadedUpdates += update
    return downloadFlow
  }

  override fun canInstall(): Boolean = canInstallResult

  override fun install(apk: File): InstallResult {
    installedApks += apk
    return installResult
  }

  companion object {
    val APK = File("PoolTrack-1.2.0.apk")

    val UPDATE =
        AppUpdate(
            version = "1.2.0",
            notes = "- Faster start",
            apkUrl = "https://github.com/SamiPr0/PoolTrack/releases/download/v1.2.0/PoolTrack.apk",
            sizeBytes = 12_900_000,
            sha256 = null,
        )
  }
}
