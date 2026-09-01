package com.example.domain.services.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import timber.log.Timber

object TermuxXeLaTeXBridge {
    private val client = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun compile(context: Context, texFile: File, fixScript: String? = null): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.i("TermuxXeLaTeXBridge.compile started for: ${texFile.absolutePath}")
            require(texFile.exists()) { "LaTeX file does not exist." }
            
            val outputDir = texFile.parentFile ?: throw IllegalArgumentException("No parent directory")
            val latexContent = texFile.readText()
            
            Timber.d("LaTeX content length: ${latexContent.length} chars")
            
            // Package payload
            val jsonBody = JSONObject().apply { 
                put("latex", latexContent)
                fixScript?.let { put("fix_script", it) } 
            }.toString()
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            
            Timber.d("Sending request to Localhost Termux Server...")
            // Dispatch to Localhost Termux Server
            val request = Request.Builder()
                .url("http://127.0.0.1:8080/compile")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown Server Error"
                Timber.e("Termux Server Error (HTTP ${response.code}): $errorMsg")
                throw Exception("Termux Compilation Failed:\n$errorMsg")
            }
            
            Timber.d("Termux Server returned success (HTTP 200). Reading PDF bytes...")
            val pdfBytes = response.body?.bytes() ?: throw Exception("Empty PDF response from server.")
            Timber.d("Received PDF bytes length: ${pdfBytes.size}")
            
            val pdfFile = File(outputDir, texFile.nameWithoutExtension + ".pdf")
            Timber.d("Writing PDF to: ${pdfFile.absolutePath}")
            
            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(pdfFile)
                fileOutputStream.write(pdfBytes)
                fileOutputStream.flush()
                Timber.i("PDF writing complete. File size: ${pdfFile.length()} bytes")
            } catch (e: Exception) {
                Timber.e(e, "Error writing PDF file to disk")
                throw e
            } finally {
                fileOutputStream?.close()
            }
            
            pdfFile
        }.onFailure { 
            Timber.e(it, "TermuxXeLaTeXBridge.compile failed")
        }
    }
}
