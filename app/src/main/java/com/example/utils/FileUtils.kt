package com.example.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

    /**
     * Sanitizes a file/directory name safely across platforms.
     * Preserves letters, digits, spaces, dashes, dots, underscores, and non-ASCII Unicode symbols.
     */
    fun getSafeFileName(name: String, fallback: String = "Project"): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return fallback

        val sanitized = trimmed.replace(Regex("[\\\\/:*?\"<>|\\x00]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (sanitized.isEmpty()) fallback else sanitized.take(128)
    }

    /**
     * Resolves a directory for exporting files safely, with fallbacks for Android 10+ scoped storage restrictions.
     */
    fun getExportDirectory(context: Context, relativePath: String = "aipdfs"): File {
        val cleanRelPath = relativePath.trimStart('/')
        
        // Attempt 1: Public Documents directory
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val exportDir = File(publicDocs, cleanRelPath)
            if (exportDir.exists() || exportDir.mkdirs()) {
                return exportDir
            }
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "Failed to access public documents directory: ${e.message}")
        }

        // Attempt 2: App external documents directory
        try {
            val externalDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (externalDocs != null) {
                val exportDir = File(externalDocs, cleanRelPath)
                if (exportDir.exists() || exportDir.mkdirs()) {
                    return exportDir
                }
            }
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "Failed to access external app files directory: ${e.message}")
        }

        // Attempt 3: App internal files directory fallback
        val internalDir = File(context.filesDir, cleanRelPath)
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        return internalDir
    }

    /**
     * Queries display name of a URI from ContentResolver.
     */
    fun getDisplayNameFromUri(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "Failed to query display name for URI $uri: ${e.message}")
        }

        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "document"
        }
        return getSafeFileName(name!!, "document")
    }

    /**
     * Queries size in bytes for a given URI. Returns -1 if unknown.
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "Failed to query size for URI $uri: ${e.message}")
        }
        return -1L
    }

    /**
     * Detects mime type or broad file category from file extension or content resolver.
     */
    fun getFileType(context: Context, uri: Uri, fileName: String? = null): FileCategory {
        val name = fileName ?: getDisplayNameFromUri(context, uri)
        val ext = name.substringAfterLast('.', "").lowercase()
        val mimeType = try { context.contentResolver.getType(uri)?.lowercase() } catch (e: Exception) { null }

        return when {
            ext == "pdf" || mimeType == "application/pdf" -> FileCategory.PDF
            ext in listOf("txt", "md", "json", "csv", "log", "tex", "html", "xml") || mimeType?.startsWith("text/") == true -> FileCategory.TEXT
            ext in listOf("png", "jpg", "jpeg", "webp", "bmp") || mimeType?.startsWith("image/") == true -> FileCategory.IMAGE
            else -> FileCategory.UNKNOWN
        }
    }

    /**
     * Purges old temporary files from cache directory.
     */
    fun cleanTempCache(context: Context, maxAgeMs: Long = 12 * 60 * 60 * 1000L) {
        try {
            val now = System.currentTimeMillis()
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") || file.name.startsWith("blueprint_")) {
                    if (now - file.lastModified() > maxAgeMs) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "Error cleaning temp cache: ${e.message}")
        }
    }

    enum class FileCategory {
        PDF, TEXT, IMAGE, UNKNOWN
    }
}
