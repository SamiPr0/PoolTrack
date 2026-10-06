package com.github.se.pooltrack.ui.update

import com.github.se.pooltrack.model.update.DownloadStatus
import com.github.se.pooltrack.model.update.InstallResult
import com.github.se.pooltrack.utils.FakeUpdateRepository
import com.github.se.pooltrack.utils.FakeUpdateRepository.Companion.APK
import com.github.se.pooltrack.utils.FakeUpdateRepository.Companion.UPDATE
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class UpdateViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private fun viewModel(repository: FakeUpdateRepository) =
      UpdateViewModel(repository, currentVersion = "1.1.0")

  @Test
  fun init_showsNothing_whenThereIsNoNewRelease() {
    val viewModel = viewModel(FakeUpdateRepository(update = null))

    assertEquals(UpdateUiState.None, viewModel.state.value)
  }

  @Test
  fun init_offersTheUpdate_whenAReleaseIsNewer() {
    val repository = FakeUpdateRepository(update = UPDATE)

    val viewModel = viewModel(repository)

    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
    assertEquals("1.1.0", repository.checkedVersion)
  }

  @Test
  fun onUpdateClick_downloadsThenOpensTheInstaller_inOneStep() {
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(listOf(UPDATE), repository.downloadedUpdates)
    assertEquals(listOf(APK), repository.installedApks)
    assertEquals(UpdateUiState.ReadyToInstall(UPDATE, APK), viewModel.state.value)
  }

  @Test
  fun onUpdateClick_showsTheDownloadProgress() {
    val downloads = MutableSharedFlow<DownloadStatus>(extraBufferCapacity = 8)
    val viewModel = viewModel(FakeUpdateRepository(update = UPDATE, downloadFlow = downloads))

    viewModel.onUpdateClick()
    assertEquals(UpdateUiState.Downloading(UPDATE, null), viewModel.state.value)

    downloads.tryEmit(DownloadStatus.Progress(0.4f))
    assertEquals(UpdateUiState.Downloading(UPDATE, 0.4f), viewModel.state.value)

    downloads.tryEmit(DownloadStatus.Finished(APK))
    assertEquals(UpdateUiState.ReadyToInstall(UPDATE, APK), viewModel.state.value)
  }

  @Test
  fun onUpdateClick_doesNothing_whenThereIsNoUpdateToInstall() {
    val repository = FakeUpdateRepository(update = null)
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(UpdateUiState.None, viewModel.state.value)
    assertEquals(emptyList<Any>(), repository.downloadedUpdates)
  }

  @Test
  fun download_fails_withTheErrorMessage() {
    val repository =
        FakeUpdateRepository(
            update = UPDATE,
            downloadFlow = flow { throw IOException("Checksum mismatch") },
        )
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(UpdateUiState.Failed(UPDATE, "Checksum mismatch"), viewModel.state.value)
    assertEquals(emptyList<Any>(), repository.installedApks)
  }

  @Test
  fun download_fails_withADefaultMessage_whenTheErrorHasNone() {
    val repository =
        FakeUpdateRepository(update = UPDATE, downloadFlow = flow { throw IOException() })
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(UpdateUiState.Failed(UPDATE, "Download failed"), viewModel.state.value)
  }

  @Test
  fun onUpdateClick_retriesTheDownload_afterAFailure() {
    val repository =
        FakeUpdateRepository(
            update = UPDATE,
            downloadFlow = flow { throw IOException("offline") },
        )
    val viewModel = viewModel(repository)
    viewModel.onUpdateClick()

    repository.downloadFlow = flow { emit(DownloadStatus.Finished(APK)) }
    viewModel.onUpdateClick()

    assertEquals(listOf(UPDATE, UPDATE), repository.downloadedUpdates)
    assertEquals(UpdateUiState.ReadyToInstall(UPDATE, APK), viewModel.state.value)
  }

  @Test
  fun install_asksForPermission_whenInstallsAreNotAllowedYet() {
    val repository =
        FakeUpdateRepository(update = UPDATE, installResult = InstallResult.PERMISSION_REQUIRED)
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(
        UpdateUiState.ReadyToInstall(UPDATE, APK, needsPermission = true),
        viewModel.state.value,
    )
  }

  @Test
  fun install_fails_whenNoInstallerCanBeOpened() {
    val repository = FakeUpdateRepository(update = UPDATE, installResult = InstallResult.FAILED)
    val viewModel = viewModel(repository)

    viewModel.onUpdateClick()

    assertEquals(
        UpdateUiState.Failed(UPDATE, "Couldn't open the installer"),
        viewModel.state.value,
    )
  }

  @Test
  fun onResume_carriesOnInstalling_onceThePermissionWasGranted() {
    val repository =
        FakeUpdateRepository(
            update = UPDATE,
            installResult = InstallResult.PERMISSION_REQUIRED,
            canInstallResult = false,
        )
    val viewModel = viewModel(repository)
    viewModel.onUpdateClick()

    // Back from the settings without having allowed anything: don't open them again.
    viewModel.onResume()
    assertEquals(listOf(APK), repository.installedApks)

    repository.canInstallResult = true
    repository.installResult = InstallResult.STARTED
    viewModel.onResume()

    assertEquals(listOf(APK, APK), repository.installedApks)
    assertEquals(UpdateUiState.ReadyToInstall(UPDATE, APK), viewModel.state.value)
  }

  @Test
  fun onResume_doesNothing_whenNoPermissionWasNeeded() {
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = viewModel(repository)
    viewModel.onUpdateClick()

    viewModel.onResume()

    assertEquals(listOf(APK), repository.installedApks)
  }

  @Test
  fun onInstallClick_opensTheInstallerAgain() {
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = viewModel(repository)
    viewModel.onUpdateClick()

    viewModel.onInstallClick()

    assertEquals(listOf(APK, APK), repository.installedApks)
  }

  @Test
  fun onInstallClick_doesNothing_beforeTheDownloadIsDone() {
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = viewModel(repository)

    viewModel.onInstallClick()

    assertEquals(emptyList<Any>(), repository.installedApks)
    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
  }

  @Test
  fun onCancelDownload_stopsTheDownload_butStillRequiresTheUpdate() {
    val downloads = MutableSharedFlow<DownloadStatus>(extraBufferCapacity = 8)
    val repository = FakeUpdateRepository(update = UPDATE, downloadFlow = downloads)
    val viewModel = viewModel(repository)
    viewModel.onUpdateClick()
    assertEquals(1, downloads.subscriptionCount.value)

    viewModel.onCancelDownload()

    assertEquals(0, downloads.subscriptionCount.value)
    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
    assertNull(repository.installedApks.firstOrNull())
  }

  @Test
  fun onCancelDownload_doesNothing_whenNotDownloading() {
    val viewModel = viewModel(FakeUpdateRepository(update = UPDATE))

    viewModel.onCancelDownload()

    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
  }

  @Test
  fun onResume_checksAgain_onceTheIntervalHasPassed() {
    var now = 0L
    val repository = FakeUpdateRepository(update = null)
    val viewModel = UpdateViewModel(repository, "1.1.0", now = { now })
    assertEquals(1, repository.checkCount)

    now = RECHECK_INTERVAL_MILLIS - 1
    viewModel.onResume()
    assertEquals(1, repository.checkCount)

    repository.update = UPDATE
    now = RECHECK_INTERVAL_MILLIS
    viewModel.onResume()

    assertEquals(2, repository.checkCount)
    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
  }

  @Test
  fun onResume_doesNotCheckAgain_whileAnUpdateIsRequired() {
    var now = 0L
    val repository = FakeUpdateRepository(update = UPDATE)
    val viewModel = UpdateViewModel(repository, "1.1.0", now = { now })

    now = RECHECK_INTERVAL_MILLIS * 2
    viewModel.onResume()

    assertEquals(1, repository.checkCount)
    assertEquals(UpdateUiState.Available(UPDATE), viewModel.state.value)
  }

  @Test
  fun onResume_staysUsable_whenTheRecheckFindsNothing() {
    var now = 0L
    val repository = FakeUpdateRepository(update = null)
    val viewModel = UpdateViewModel(repository, "1.1.0", now = { now })

    now = RECHECK_INTERVAL_MILLIS
    viewModel.onResume()

    assertEquals(2, repository.checkCount)
    assertEquals(UpdateUiState.None, viewModel.state.value)
  }
}
