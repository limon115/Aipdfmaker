package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
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

    /**
     * In-memory cache for compiled PDF file lookups to prevent blocking disk I/O on the UI thread during LazyColumn scrolling.
     */
    private val NOT_FOUND_FILE = File("")
    private val compiledPdfCache = java.util.concurrent.ConcurrentHashMap<String, File>()

    fun invalidateCompiledPdfCache() {
        compiledPdfCache.clear()
    }

    /**
     * Resolves the dedicated directory for Math Solver outputs:
     * "/storage/emulated/0/Documents/aipdfs/mathsolver"
     */
    fun getMathSolverDirectory(context: Context): File {
        return getExportDirectory(context, "aipdfs/mathsolver")
    }

    /**
     * Locates any compiled PDF associated with a project.
     * Looks in public, external, and internal directories under "aipdfs/mathsolver", "aipdfs/<safeName>", and "AiPdfMaker".
     * Uses in-memory caching to prevent stutter and frame drops while scrolling LazyColumns.
     */
    fun findCompiledPdf(context: Context, projectTitle: String, lastUpdated: Long = 0L): File? {
        val cacheKey = "${projectTitle}_${lastUpdated}"
        val cached = compiledPdfCache[cacheKey]
        if (cached != null) {
            return if (cached === NOT_FOUND_FILE || !cached.exists() || cached.length() <= 0) {
                null
            } else {
                cached
            }
        }

        val safeName = getSafeFileName(projectTitle, "Project")
        val strippedTitle = getSafeFileName(
            projectTitle.removePrefix("Math:").removePrefix("Math -").trim(),
            ""
        )
        val possibleDirs = mutableListOf<File>()

        // 1. Primary export directory for this project
        try {
            possibleDirs.add(getExportDirectory(context, "aipdfs/$safeName"))
        } catch (_: Exception) {}

        // 2. Math solver dedicated directory (/storage/emulated/0/Documents/aipdfs/mathsolver)
        try {
            possibleDirs.add(getMathSolverDirectory(context))
        } catch (_: Exception) {}

        // 3. Public Documents directory
        try {
            val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            possibleDirs.add(File(pub, "aipdfs/$safeName"))
            possibleDirs.add(File(pub, "aipdfs/mathsolver"))
            possibleDirs.add(File(pub, "AiPdfMaker"))
        } catch (_: Exception) {}

        // 4. External app documents
        try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let {
                possibleDirs.add(File(it, "aipdfs/$safeName"))
                possibleDirs.add(File(it, "aipdfs/mathsolver"))
                possibleDirs.add(File(it, "AiPdfMaker"))
            }
        } catch (_: Exception) {}

        // 5. Internal app files
        possibleDirs.add(File(context.filesDir, "aipdfs/$safeName"))
        possibleDirs.add(File(context.filesDir, "aipdfs/mathsolver"))
        possibleDirs.add(File(context.filesDir, "AiPdfMaker"))

        val targetNames = mutableListOf("main.pdf", "document.pdf", "solution.pdf", "solution_native.pdf", "$safeName.pdf")
        if (strippedTitle.isNotBlank()) {
            targetNames.add("$strippedTitle.pdf")
        }

        for (dir in possibleDirs) {
            if (!dir.exists() || !dir.isDirectory) continue

            // Check specific target file names first
            for (name in targetNames) {
                val candidate = File(dir, name)
                if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                    compiledPdfCache[cacheKey] = candidate
                    return candidate
                }
            }

            // Check any other .pdf files in this directory
            val pdfs = dir.listFiles { file ->
                file.isFile && file.name.endsWith(".pdf", ignoreCase = true) && file.length() > 0
            }
            if (!pdfs.isNullOrEmpty()) {
                if (dir.name == "mathsolver") {
                    val matched = pdfs.firstOrNull {
                        it.nameWithoutExtension.equals(safeName, ignoreCase = true) ||
                        (strippedTitle.isNotBlank() && it.nameWithoutExtension.contains(strippedTitle.take(15), ignoreCase = true))
                    }
                    if (matched != null) {
                        compiledPdfCache[cacheKey] = matched
                        return matched
                    }
                } else if (dir.name == "AiPdfMaker") {
                    val cleanWord = safeName.replace("Math_", "").replace("Project", "").trim('_')
                    val matched = if (cleanWord.isNotBlank()) {
                        pdfs.firstOrNull { it.name.contains(cleanWord.take(15), ignoreCase = true) }
                    } else null
                    if (matched != null) {
                        compiledPdfCache[cacheKey] = matched
                        return matched
                    }
                } else {
                    val latest = pdfs.maxByOrNull { it.lastModified() }
                    if (latest != null) {
                        compiledPdfCache[cacheKey] = latest
                        return latest
                    }
                }
            }
        }
        compiledPdfCache[cacheKey] = NOT_FOUND_FILE
        return null
    }

    /**
     * Replaces previous compiled PDF and companion files for a project with newly compiled output.
     * Ensures any previous PDF file (in project dir, mathsolver dir, public Documents, or custom location)
     * is completely replaced so that subsequent opens, exports, and previews reflect the latest compilation.
     */
    fun replacePreviousCompiledPdf(
        context: Context,
        projectTitle: String,
        newPdf: File,
        newTex: File? = null,
        previousPdf: File? = null
    ) {
        val safeName = getSafeFileName(projectTitle, "Project")
        val strippedTitle = getSafeFileName(
            projectTitle.removePrefix("Math:").removePrefix("Math -").trim(),
            ""
        )
        val updatedFiles = mutableListOf<File>()
        updatedFiles.add(newPdf)

        // 1. If an explicit previous PDF path was tracked and exists, overwrite it directly
        if (previousPdf != null && previousPdf.exists() && previousPdf.canonicalPath != newPdf.canonicalPath) {
            try {
                newPdf.copyTo(previousPdf, overwrite = true)
                updatedFiles.add(previousPdf)
                AppLogger.i("FileUtils", "Replaced explicit previous PDF at: ${previousPdf.absolutePath}")
                if (newTex != null && newTex.exists()) {
                    val companionTex = File(previousPdf.parentFile, "${previousPdf.nameWithoutExtension}.tex")
                    if (companionTex.exists() && companionTex.canonicalPath != newTex.canonicalPath) {
                        newTex.copyTo(companionTex, overwrite = true)
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("FileUtils", "Failed to overwrite explicit previous PDF: ${e.message}")
            }
        }

        // 2. Overwrite any alternate/legacy PDF names in the project's export directory
        val baseDir = try { getExportDirectory(context, "aipdfs/$safeName") } catch (_: Exception) { null }
        if (baseDir != null && baseDir.exists()) {
            val legacyNames = listOf("document.pdf", "$safeName.pdf", "solution.pdf", "solution_native.pdf", "main.pdf")
            for (name in legacyNames) {
                val candidate = File(baseDir, name)
                if (candidate.exists() && candidate.isFile && candidate.canonicalPath != newPdf.canonicalPath) {
                    try {
                        newPdf.copyTo(candidate, overwrite = true)
                        updatedFiles.add(candidate)
                        AppLogger.i("FileUtils", "Replaced legacy PDF variant: ${candidate.absolutePath}")
                    } catch (e: Exception) {
                        AppLogger.w("FileUtils", "Failed to overwrite legacy PDF $name: ${e.message}")
                    }
                }
            }
            if (newTex != null && newTex.exists()) {
                val legacyTexNames = listOf("document.tex", "$safeName.tex", "solution.tex", "main.tex")
                for (tName in legacyTexNames) {
                    val texCandidate = File(baseDir, tName)
                    if (texCandidate.exists() && texCandidate.isFile && texCandidate.canonicalPath != newTex.canonicalPath) {
                        try {
                            newTex.copyTo(texCandidate, overwrite = true)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // 3. Overwrite matching PDFs in dedicated Math Solver directory if applicable
        val mathDir = try { getMathSolverDirectory(context) } catch (_: Exception) { null }
        if (mathDir != null && mathDir.exists()) {
            val mathCandidates = mathDir.listFiles { f ->
                f.isFile && f.name.endsWith(".pdf", ignoreCase = true) && (
                    f.nameWithoutExtension.equals(safeName, ignoreCase = true) ||
                    (strippedTitle.isNotBlank() && f.nameWithoutExtension.equals(strippedTitle, ignoreCase = true)) ||
                    (strippedTitle.isNotBlank() && f.nameWithoutExtension.contains(strippedTitle.take(15), ignoreCase = true))
                )
            }
            mathCandidates?.forEach { mathPdf ->
                if (mathPdf.canonicalPath != newPdf.canonicalPath) {
                    try {
                        newPdf.copyTo(mathPdf, overwrite = true)
                        updatedFiles.add(mathPdf)
                        AppLogger.i("FileUtils", "Replaced mathsolver PDF at: ${mathPdf.absolutePath}")
                        if (newTex != null && newTex.exists()) {
                            val mathTex = File(mathDir, "${mathPdf.nameWithoutExtension}.tex")
                            if (mathTex.exists() && mathTex.canonicalPath != newTex.canonicalPath) {
                                newTex.copyTo(mathTex, overwrite = true)
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("FileUtils", "Failed to overwrite mathsolver PDF: ${e.message}")
                    }
                }
            }
        }

        // 4. Overwrite matching PDFs in AiPdfMaker / Public / External dirs
        try {
            val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val aiPdfMakerDir = File(pub, "AiPdfMaker")
            if (aiPdfMakerDir.exists() && aiPdfMakerDir.isDirectory) {
                val cleanWord = safeName.replace("Math_", "").replace("Project", "").trim('_')
                if (cleanWord.isNotBlank()) {
                    val matched = aiPdfMakerDir.listFiles { f ->
                        f.isFile && f.name.endsWith(".pdf", ignoreCase = true) && f.name.contains(cleanWord.take(15), ignoreCase = true)
                    }
                    matched?.forEach { oldFile ->
                        if (oldFile.canonicalPath != newPdf.canonicalPath) {
                            try {
                                newPdf.copyTo(oldFile, overwrite = true)
                                updatedFiles.add(oldFile)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. Invalidate the in-memory findCompiledPdf cache
        invalidateCompiledPdfCache()

        // 6. Notify MediaScanner for all updated paths
        try {
            val paths = updatedFiles.map { it.absolutePath }.toTypedArray()
            android.media.MediaScannerConnection.scanFile(
                context,
                paths,
                Array(paths.size) { "application/pdf" },
                null
            )
        } catch (e: Exception) {
            AppLogger.w("FileUtils", "MediaScanner notification failed: ${e.message}")
        }
    }

    /**
     * Opens a PDF file using the system's default or chooser PDF viewer.
     */
    fun openPdf(context: Context, file: File, chooserTitle: String = "Open PDF") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            AppLogger.e("FileUtils", "Failed to open PDF: ${e.message}", e)
            Toast.makeText(context, "Could not open PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats a byte count into human-readable string (KB, MB, etc.).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    enum class FileCategory {
        PDF, TEXT, IMAGE, UNKNOWN
    }
}
