package com.example.ui.screens.updater

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.services.updater.ApkExtractorService
import com.example.domain.services.updater.ApkInstallerService
import com.example.domain.services.updater.ApkValidationService
import com.example.domain.services.updater.GitHubArtifact
import com.example.domain.services.updater.GitHubArtifactService
import com.example.domain.services.updater.UpdateState
import com.example.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppUpdateViewModel"
    }

    private val dataStore = AiSettingsDataStore(application)
    private val artifactService = GitHubArtifactService(application)
    private val extractorService = ApkExtractorService()
    private val validationService = ApkValidationService(application)
    private val installerService = ApkInstallerService(application)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    val currentVersionName: String
    val currentVersionCode: Long

    init {
        val packageInfo = try {
            application.packageManager.getPackageInfo(application.packageName, 0)
        } catch (e: Exception) {
            null
        }
        currentVersionName = packageInfo?.versionName ?: "1.0"
        currentVersionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 1L

        // Clean up any update files leftover from a previous installation
        ApkInstallerService.cleanupUpdatesDir(application)

        viewModelScope.launch {
            dataStore.aiSettingsFlow.collectLatest { settings ->
                _githubToken.value = settings.githubToken
            }
        }
    }

    fun saveGithubToken(token: String) {
        viewModelScope.launch {
            dataStore.updateGithubToken(token)
            _githubToken.value = token
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) {
            return
        }

        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            AppLogger.i(TAG, "Checking for updates from GitHub Actions...")
            val result = artifactService.checkForUpdate(_githubToken.value)
            result.onSuccess { updateInfo ->
                if (updateInfo.isNewer) {
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                } else {
                    _updateState.value = UpdateState.UpToDate(
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode
                    )
                }
            }.onFailure { error ->
                val isAuth = error.message?.contains("authorization", ignoreCase = true) == true ||
                        error.message?.contains("401", ignoreCase = true) == true ||
                        error.message?.contains("403", ignoreCase = true) == true
                _updateState.value = UpdateState.Error(
                    message = error.message ?: "Failed to check for updates.",
                    isAuthError = isAuth
                )
            }
        }
    }

    fun downloadAndPrepareUpdate(artifact: GitHubArtifact) {
        if (_githubToken.value.isBlank()) {
            _updateState.value = UpdateState.Error(
                message = "A GitHub Personal Access Token (PAT) is required to download artifacts from GitHub Actions. Please enter your PAT below.",
                isAuthError = true
            )
            return
        }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val updatesDir = File(context.filesDir, "updates")
                if (!updatesDir.exists()) updatesDir.mkdirs()

                val zipFile = File(updatesDir, "update-artifact-${artifact.id}.zip")

                _updateState.value = UpdateState.Downloading(
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = artifact.sizeInBytes
                )

                // 1. Download artifact ZIP
                val downloadResult = artifactService.downloadArtifactZip(
                    artifact = artifact,
                    token = _githubToken.value,
                    outputZipFile = zipFile,
                    onProgress = { downloaded, total, speed ->
                        val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                        _updateState.value = UpdateState.Downloading(
                            progress = progress.coerceIn(0f, 1f),
                            bytesDownloaded = downloaded,
                            totalBytes = total,
                            speedBytesPerSec = speed
                        )
                    }
                )

                if (downloadResult.isFailure) {
                    val err = downloadResult.exceptionOrNull()
                    val isAuth = err?.message?.contains("authentication", ignoreCase = true) == true ||
                            err?.message?.contains("401") == true || err?.message?.contains("403") == true
                    _updateState.value = UpdateState.Error(
                        message = err?.message ?: "Failed to download update ZIP.",
                        isAuthError = isAuth
                    )
                    return@launch
                }

                // 2. Extract APK from ZIP
                _updateState.value = UpdateState.Extracting
                val extractResult = extractorService.extractApkFromZip(
                    zipFile = zipFile,
                    destinationDir = updatesDir,
                    targetApkName = "DocMorph-update.apk"
                )

                if (extractResult.isFailure) {
                    val err = extractResult.exceptionOrNull()
                    _updateState.value = UpdateState.Error(
                        message = err?.message ?: "Failed to extract APK from ZIP archive."
                    )
                    return@launch
                }

                val apkFile = extractResult.getOrThrow()

                // 3. Validate APK Package & Signatures
                _updateState.value = UpdateState.Validating
                val validationResult = validationService.validateApk(apkFile)

                if (!validationResult.isValid) {
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }
                    _updateState.value = UpdateState.Error(
                        message = "APK Validation Failed: ${validationResult.statusMessage}"
                    )
                    return@launch
                }

                // 4. Ready to Install
                _updateState.value = UpdateState.ReadyToInstall(
                    apkFile = apkFile,
                    validation = validationResult
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error during update preparation", e)
                _updateState.value = UpdateState.Error(
                    message = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun installApk(apkFile: File) {
        _updateState.value = UpdateState.Installing
        val result = installerService.installApk(apkFile)
        if (result.isFailure) {
            _updateState.value = UpdateState.Error(
                message = "Installation failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    /**
     * Checks if the app was newly installed after returning to the app and cleans up storage.
     */
    fun onResumeCheck() {
        val context = getApplication<Application>()
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
        val latestCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: currentVersionCode
        if (latestCode > currentVersionCode) {
            AppLogger.i(TAG, "Detected newly installed version: $latestCode > $currentVersionCode. Cleaning up update files.")
            ApkInstallerService.cleanupUpdatesDir(context)
            _updateState.value = UpdateState.UpToDate(
                currentVersionName = packageInfo?.versionName ?: currentVersionName,
                currentVersionCode = latestCode
            )
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return installerService.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        installerService.openUnknownSourcesSettings()
    }

    fun resetState() {
        ApkInstallerService.cleanupUpdatesDir(getApplication())
        _updateState.value = UpdateState.Idle
    }
}
