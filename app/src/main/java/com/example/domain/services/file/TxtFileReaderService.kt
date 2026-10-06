package com.example.domain.services.file

import android.content.Context
import android.net.Uri
import com.example.utils.AppLogger
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TxtFileReaderService {

    /**
     * Reads text content safely from a given URI with charset detection, BOM handling, size limits, and fallback strategies.
     */
    suspend fun readTextFromUri(
        uri: Uri,
        context: Context,
        maxSizeBytes: Long = 10 * 1024 * 1024L
    ): String = withContext(Dispatchers.IO) {
        val fileSize = FileUtils.getFileSizeFromUri(context, uri)
        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("File size exceeds limit (${fileSize / (1024 * 1024)}MB > ${maxSizeBytes / (1024 * 1024)}MB)")
        }

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open input stream for URI: $uri")

        try {
            val bytes = inputStream.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (stream.read(chunk).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                    if (totalRead > maxSizeBytes) {
                        throw IllegalArgumentException("File content exceeds limit during read.")
                    }
                    buffer.write(chunk, 0, bytesRead)
                }
                buffer.toByteArray()
            }

            if (bytes.isEmpty()) {
                return@withContext ""
            }

            val charset = detectCharsetAndSkipBom(bytes)
            val text = String(bytes, charset.bomLength, bytes.size - charset.bomLength, charset.charset)
            
            // Normalize line endings and trim null characters
            return@withContext text.replace("\r\n", "\n").replace("\r", "\n").replace("\u0000", "")
        } catch (e: Exception) {
            AppLogger.e("TxtFileReaderService", "Error reading text from URI $uri", e)
            throw e
        }
    }

    private data class CharsetInfo(val charset: Charset, val bomLength: Int)

    private fun detectCharsetAndSkipBom(bytes: ByteArray): CharsetInfo {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return CharsetInfo(StandardCharsets.UTF_8, 3)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return CharsetInfo(StandardCharsets.UTF_16BE, 2)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return CharsetInfo(StandardCharsets.UTF_16LE, 2)
        }
        return CharsetInfo(StandardCharsets.UTF_8, 0)
    }
}
