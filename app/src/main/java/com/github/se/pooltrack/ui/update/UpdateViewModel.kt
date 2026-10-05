package com.github.se.pooltrack.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.BuildConfig
import com.github.se.pooltrack.model.update.AppUpdate
import com.github.se.pooltrack.model.update.DownloadStatus
import com.github.se.pooltrack.model.update.InstallResult
import com.github.se.pooltrack.model.update.UpdateRepository
import com.github.se.pooltrack.model.update.UpdateRepositoryProvider
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** What the update prompt currently shows. */
sealed interface UpdateUiState {
  /** Nothing to show: the app is up to date, the check failed, or the user dismissed the prompt. */
  data object None : UpdateUiState

  data class Available(val update: AppUpdate) : UpdateUiState

  /** [progress] is in `0..1`, or `null` while the total size is not known yet. */
  data class Downloading(val update: AppUpdate, val progress: Float?) : UpdateUiState

  /**
   * The APK is downloaded. [needsPermission] is true when installing was blocked because the user
   * still has to allow PoolTrack to install apps.
   */
  data class ReadyToInstall(
      val update: AppUpdate,
      val apk: File,
      val needsPermission: Boolean = false,
  ) : UpdateUiState

  data class Failed(val update: AppUpdate, val message: String) : UpdateUiState
}

/**
 * ViewModel behind the update prompt. It checks for a new release once, when it is created, then
 * walks the user through downloading and installing it: the installer opens by itself as soon as
 * the download is done, so updating takes a single tap.
 *
 * @property repository Where releases are found, downloaded and installed.
 * @property currentVersion The version of the running app.
 */
class UpdateViewModel(
    private val repository: UpdateRepository = UpdateRepositoryProvider.repository,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) : ViewModel() {

  private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.None)
  val state: StateFlow<UpdateUiState> = _state.asStateFlow()

  private var downloadJob: Job? = null

  init {
    viewModelScope.launch {
      repository.checkForUpdate(currentVersion)?.let { _state.value = UpdateUiState.Available(it) }
    }
  }

  /** Starts downloading the update, or retries after a failed download. */
  fun onUpdateClick() {
    val update =
        when (val current = _state.value) {
          is UpdateUiState.Available -> current.update
          is UpdateUiState.Failed -> current.update
          else -> return
        }
    download(update)
  }

  /** Opens the system installer for an already downloaded update. */
  fun onInstallClick() {
    val current = _state.value as? UpdateUiState.ReadyToInstall ?: return
    install(current.update, current.apk)
  }

  /**
   * To be called whenever the app comes back to the foreground. If the user was sent to the
   * settings to allow installs and did so, the installation carries on without another tap.
   */
  fun onResume() {
    val current = _state.value as? UpdateUiState.ReadyToInstall ?: return
    if (current.needsPermission && repository.canInstall()) install(current.update, current.apk)
  }

  /** Hides the prompt, cancelling the download if there is one. It reappears on the next launch. */
  fun onDismiss() {
    downloadJob?.cancel()
    downloadJob = null
    _state.value = UpdateUiState.None
  }

  private fun download(update: AppUpdate) {
    downloadJob?.cancel()
    _state.value = UpdateUiState.Downloading(update, progress = null)
    downloadJob = viewModelScope.launch {
      repository
          .download(update)
          .catch { error ->
            _state.value = UpdateUiState.Failed(update, error.message ?: "Download failed")
          }
          .collect { status ->
            when (status) {
              is DownloadStatus.Progress ->
                  _state.value = UpdateUiState.Downloading(update, status.fraction)
              is DownloadStatus.Finished -> install(update, status.apk)
            }
          }
    }
  }

  private fun install(update: AppUpdate, apk: File) {
    _state.value =
        when (repository.install(apk)) {
          InstallResult.STARTED -> UpdateUiState.ReadyToInstall(update, apk)
          InstallResult.PERMISSION_REQUIRED ->
              UpdateUiState.ReadyToInstall(update, apk, needsPermission = true)
          InstallResult.FAILED -> UpdateUiState.Failed(update, "Couldn't open the installer")
        }
  }
}
