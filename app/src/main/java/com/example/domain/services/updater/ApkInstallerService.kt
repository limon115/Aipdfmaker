package com.example.domain.services.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.utils.AppLogger
import java.io.File

class ApkInstallerService(private val context: Context) {

    companion object {
        private const val TAG = "ApkInstallerService"

        /**
         * Cleans up all downloaded update files (APKs, ZIPs, and temporary files) from storage.
         */
        fun cleanupUpdatesDir(context: Context) {
            try {
                val updatesDir = File(context.filesDir, "updates")
                if (updatesDir.exists() && updatesDir.isDirectory) {
                    val files = updatesDir.listFiles()
                    files?.forEach { file ->
                        if (file.isFile) {
                            val deleted = file.delete()
                            AppLogger.i(TAG, "Cleaned up update file from storage: ${file.name}, success=$deleted")
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to clean up update files from storage", e)
            }
        }
    }

    /**
     * Checks whether the application has permission to request package installations.
     * Always returns true on Android versions lower than 8.0 (API 26).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Directs the user to system settings to grant the REQUEST_INSTALL_PACKAGES permission.
     */
    fun openUnknownSourcesSettings() {
        AppLogger.i(TAG, "Opening unknown app sources settings")
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open settings for unknown app sources", e)
        }
    }

    /**
     * Initiates the system package installer for the given validated APK file.
     * Uses Android FileProvider to securely grant read URI permission to the system installer.
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                val errorMsg = "Cannot install: APK file does not exist at ${apkFile.absolutePath}"
                AppLogger.e(TAG, errorMsg)
                return Result.failure(IllegalArgumentException(errorMsg))
            }

            try {
                apkFile.deleteOnExit()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not set deleteOnExit: ${e.message}")
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            AppLogger.i(TAG, "Launching package installer for URI: $apkUri")

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to launch package installer", e)
            Result.failure(e)
        }
    }
}
