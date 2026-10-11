package com.example.domain.services.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocalOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return extractTextFromBitmapResult(bitmap).getOrDefault("")
    }

    suspend fun extractTextFromBitmapResult(bitmap: Bitmap): Result<String> {
        var retries = 0
        while (retries < 30) {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                return Result.success(result.text)
            } catch (e: Exception) {
                if (e.message?.contains("Waiting for the text optional module to be downloaded") == true) {
                    retries++
                    kotlinx.coroutines.delay(2000)
                } else {
                    com.example.utils.AppLogger.e("LocalOcrEngine", "Error recognizing text from bitmap: ${e.message}", e)
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(IllegalStateException("OCR model download timed out. Please check your internet connection."))
    }
    
    suspend fun extractTextFromBitmaps(bitmaps: List<Bitmap>): String {
        val textBuilder = StringBuilder()
        for (bitmap in bitmaps) {
            val res = extractTextFromBitmapResult(bitmap)
            if (res.isSuccess) {
                val text = res.getOrDefault("")
                if (text.isNotBlank()) {
                    textBuilder.append(text).append("\n\n")
                }
            }
        }
        return textBuilder.toString().trim()
    }

    suspend fun extractTextFromImageUri(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        var retries = 0
        while (retries < 30) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val result = recognizer.process(image).await()
                return@withContext result.text
            } catch (e: Exception) {
                if (e.message?.contains("Waiting for the text optional module to be downloaded") == true) {
                    retries++
                    kotlinx.coroutines.delay(2000)
                } else {
                    com.example.utils.AppLogger.e("LocalOcrEngine", "Error extracting text from image URI $uri: ${e.message}", e)
                    throw e
                }
            }
        }
        throw IllegalStateException("OCR model download timed out. Please check your internet connection and try again.")
    }
}
