package com.github.se.pooltrack.model.update

import java.io.File
import kotlinx.coroutines.flow.Flow

/** The progress of an APK download, as reported by [UpdateRepository.download]. */
sealed interface DownloadStatus {
  /** [fraction] is in `0..1`, or `null` when the total size is unknown. */
  data class Progress(val fraction: Float?) : DownloadStatus

  /** The APK was fully downloaded and verified, and is ready to install. */
  data class Finished(val apk: File) : DownloadStatus
}

/** What happened when asking the system to install a downloaded APK. */
enum class InstallResult {
  /** The system installer was opened. */
  STARTED,
  /** The user has to allow PoolTrack to install apps first; the relevant settings were opened. */
  PERMISSION_REQUIRED,
  /** No installer could be opened. */
  FAILED,
}

/** Finds, downloads and installs new versions of the app. */
interface UpdateRepository {

  /**
   * Returns the latest release if it is newer than [currentVersion], or `null` if the app is up to
   * date or the check failed (e.g. offline). It never throws, since a failed check is not worth
   * bothering the user about.
   */
  suspend fun checkForUpdate(currentVersion: String): AppUpdate?

  /**
   * Downloads and verifies the APK of [update], emitting its progress. Fails with an
   * [java.io.IOException] if the download or the verification fails.
   */
  fun download(update: AppUpdate): Flow<DownloadStatus>

  /** Whether the user already allowed PoolTrack to install apps. */
  fun canInstall(): Boolean

  /** Asks the system to install [apk] over the running app. */
  fun install(apk: File): InstallResult
}
