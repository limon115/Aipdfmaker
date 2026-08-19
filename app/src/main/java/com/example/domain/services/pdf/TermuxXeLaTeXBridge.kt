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
import java.util.concurrent.TimeUnit

object TermuxXeLaTeXBridge {

    private val client = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun compile(context: Context, texFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(texFile.exists()) { "LaTeX file does not exist." }
            
            val outputDir = texFile.parentFile ?: throw IllegalArgumentException("No parent directory")
            val latexContent = texFile.readText()
            
            // Package payload
            val jsonBody = JSONObject().apply { put("latex", latexContent) }.toString()
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            
            // Dispatch to Localhost Termux Server
            val request = Request.Builder()
                .url("http://127.0.0.1:8080/compile")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown Server Error"
                throw Exception("Termux Compilation Failed:\n$errorMsg")
            }

            val pdfBytes = response.body?.bytes() ?: throw Exception("Empty PDF response from server.")
            
            val pdfFile = File(outputDir, texFile.nameWithoutExtension + ".pdf")
            pdfFile.writeBytes(pdfBytes)

            pdfFile
        }
    }
}
