package com.example.domain.services.updater

import com.example.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ApkExtractorService {

    companion object {
        private const val TAG = "ApkExtractorService"
    }

    /**
     * Extracts the target APK from the downloaded GitHub Actions ZIP archive.
     * Prevents Zip Slip vulnerabilities and cleans up the input zip file upon completion.
     */
    suspend fun extractApkFromZip(
        zipFile: File,
        destinationDir: File,
        targetApkName: String = "DocMorph-update.apk"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return@withContext Result.failure(
                    IllegalArgumentException("Zip file does not exist or is empty.")
                )
            }

            destinationDir.mkdirs()
            val canonicalDestDir = destinationDir.canonicalPath
            val targetApkFile = File(destinationDir, targetApkName)
            if (targetApkFile.exists()) {
                targetApkFile.delete()
            }

            var foundApk = false
            var extractedBytes = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile), 128 * 1024)).use { zipIn ->
                var entry = zipIn.nextEntry
                val buffer = ByteArray(128 * 1024)

                while (entry != null) {
                    val entryName = entry.name
                    val destinationFile = File(destinationDir, entryName)
                    val canonicalPath = destinationFile.canonicalPath

                    // Zip Slip vulnerability check
                    if (!canonicalPath.startsWith(canonicalDestDir)) {
                        throw SecurityException("Malicious ZIP entry detected (escapes root): $entryName")
                    }

                    if (!entry.isDirectory && entryName.endsWith(".apk", ignoreCase = true)) {
                        AppLogger.i(TAG, "Extracting APK entry: $entryName (${entry.size} bytes)")
                        BufferedOutputStream(FileOutputStream(targetApkFile), 128 * 1024).use { out ->
                            var len: Int
                            while (zipIn.read(buffer).also { len = it } != -1) {
                                out.write(buffer, 0, len)
                                extractedBytes += len
                            }
                            out.flush()
                        }
                        foundApk = true
                        zipIn.closeEntry()
                        break
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Cleanup the downloaded zip archive to save device disk space
            try {
                if (zipFile.exists()) {
                    zipFile.delete()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not delete temporary zip: ${e.message}")
            }

            if (!foundApk || !targetApkFile.exists() || targetApkFile.length() == 0L) {
                return@withContext Result.failure(
                    IllegalStateException("No valid .apk file found inside artifact ZIP archive.")
                )
            }

            AppLogger.i(TAG, "APK successfully extracted to ${targetApkFile.absolutePath} ($extractedBytes bytes)")
            Result.success(targetApkFile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to extract APK from ZIP", e)
            Result.failure(e)
        }
    }
}
