package com.example.domain.services.updater

import java.io.File

/**
 * Metadata for a GitHub Actions workflow artifact.
 */
data class GitHubArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val archiveDownloadUrl: String,
    val createdAt: String,
    val workflowRunId: Long,
    val headSha: String,
    val headBranch: String
)

/**
 * Information comparing the remote update artifact with the locally installed app version.
 */
data class UpdateInfo(
    val artifact: GitHubArtifact,
    val currentVersionName: String,
    val currentVersionCode: Long,
    val isNewer: Boolean,
    val formattedSize: String,
    val formattedDate: String
)

/**
 * Result of inspecting and validating a downloaded APK file before installation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isPackageMatch: Boolean,
    val isSignatureMatch: Boolean,
    val statusMessage: String
)

/**
 * UI / workflow state for the Android updater.
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateState()
    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Long,
        val lastChecked: Long = System.currentTimeMillis()
    ) : UpdateState()
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data object Extracting : UpdateState()
    data object Validating : UpdateState()
    data class ReadyToInstall(
        val apkFile: File,
        val validation: ValidationResult
    ) : UpdateState()
    data object Installing : UpdateState()
    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : UpdateState()
}
