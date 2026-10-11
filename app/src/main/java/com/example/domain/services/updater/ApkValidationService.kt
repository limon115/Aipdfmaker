package com.example.domain.services.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.example.utils.AppLogger
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class ApkValidationService(private val context: Context) {

    companion object {
        private const val TAG = "ApkValidationService"
        const val EXPECTED_PACKAGE_NAME = "com.aistudio.docmorph.kqbzpa"
    }

    /**
     * Inspects and validates a downloaded APK file before installation.
     * Checks package name match, extracts version info, and compares signer signatures.
     */
    fun validateApk(apkFile: File): ValidationResult {
        AppLogger.i(TAG, "Starting validation for APK: ${apkFile.absolutePath}")

        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ValidationResult(
                isValid = false,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                isPackageMatch = false,
                isSignatureMatch = false,
                statusMessage = "APK file does not exist or is empty."
            )
        }

        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = try {
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse APK archive info", e)
            null
        }

        if (archiveInfo == null) {
            return ValidationResult(
                isValid = false,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                isPackageMatch = false,
                isSignatureMatch = false,
                statusMessage = "Unable to parse APK archive. File may be corrupted or incomplete."
            )
        }

        archiveInfo.applicationInfo?.sourceDir = apkFile.absolutePath
        archiveInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath

        val apkPackageName = archiveInfo.packageName ?: ""
        val apkVersionName = archiveInfo.versionName ?: "unknown"
        val apkVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)

        // 1. Verify Package Name
        val isPackageMatch = apkPackageName == EXPECTED_PACKAGE_NAME || apkPackageName == context.packageName
        if (!isPackageMatch) {
            val errorMsg = "Package mismatch: APK is '$apkPackageName', expected '$EXPECTED_PACKAGE_NAME'"
            AppLogger.w(TAG, errorMsg)
            return ValidationResult(
                isValid = false,
                packageName = apkPackageName,
                versionName = apkVersionName,
                versionCode = apkVersionCode,
                isPackageMatch = false,
                isSignatureMatch = false,
                statusMessage = errorMsg
            )
        }

        // 2. Verify Signatures
        val signatureMatchResult = verifySignatures(packageManager, archiveInfo)
        if (!signatureMatchResult.isMatch) {
            AppLogger.w(TAG, "Signature verification failed: ${signatureMatchResult.reason}")
            return ValidationResult(
                isValid = false,
                packageName = apkPackageName,
                versionName = apkVersionName,
                versionCode = apkVersionCode,
                isPackageMatch = true,
                isSignatureMatch = false,
                statusMessage = signatureMatchResult.reason
            )
        }

        AppLogger.i(TAG, "APK successfully validated: version=$apkVersionName ($apkVersionCode)")
        return ValidationResult(
            isValid = true,
            packageName = apkPackageName,
            versionName = apkVersionName,
            versionCode = apkVersionCode,
            isPackageMatch = true,
            isSignatureMatch = true,
            statusMessage = "APK signature and package verified successfully."
        )
    }

    private data class SignatureMatchResult(val isMatch: Boolean, val reason: String)

    private fun verifySignatures(packageManager: PackageManager, archiveInfo: PackageInfo): SignatureMatchResult {
        return try {
            val currentPackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val currentSignatures = getSignatures(currentPackageInfo)
            val archiveSignatures = getSignatures(archiveInfo)

            if (currentSignatures.isEmpty()) {
                AppLogger.w(TAG, "No installed signatures found (development mode)")
                return SignatureMatchResult(true, "Current package has no signatures, bypassed.")
            }

            if (archiveSignatures.isEmpty()) {
                return SignatureMatchResult(false, "APK contains no signing certificates.")
            }

            val currentHashes = currentSignatures.map { sha256Fingerprint(it) }.toSet()
            val archiveHashes = archiveSignatures.map { sha256Fingerprint(it) }.toSet()

            // Check if any signing key matches
            val hasMatchingKey = archiveHashes.any { currentHashes.contains(it) }

            if (hasMatchingKey) {
                SignatureMatchResult(true, "Signatures match.")
            } else {
                val installedPrefix = currentHashes.firstOrNull()?.take(16) ?: "unknown"
                val archivePrefix = archiveHashes.firstOrNull()?.take(16) ?: "unknown"
                SignatureMatchResult(
                    false,
                    "Signature mismatch! Installed: $installedPrefix... vs APK: $archivePrefix...\nAndroid requires identical signing keys to update. If your installed version was built with a different keystore, reinstall the app once to align certificates."
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error checking signatures", e)
            SignatureMatchResult(false, "Signature verification error: ${e.message}")
        }
    }

    private fun getSignatures(packageInfo: PackageInfo): Array<Signature> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                return if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners ?: emptyArray()
                } else {
                    signingInfo.signingCertificateHistory ?: emptyArray()
                }
            }
        }
        @Suppress("DEPRECATION")
        return packageInfo.signatures ?: emptyArray()
    }

    fun sha256Fingerprint(signature: Signature): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature.toByteArray())
            digest.joinToString(":") { "%02X".format(Locale.US, it) }
        } catch (e: Exception) {
            ""
        }
    }
}
