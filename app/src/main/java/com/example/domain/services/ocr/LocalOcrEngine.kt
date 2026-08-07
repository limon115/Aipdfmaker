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
        var retries = 0
        while (retries < 30) {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                return result.text
            } catch (e: Exception) {
                if (e.message?.contains("Waiting for the text optional module to be downloaded") == true) {
                    retries++
                    kotlinx.coroutines.delay(2000)
                } else {
                    e.printStackTrace()
                    return "Error extracting text: ${e.message}"
                }
            }
        }
        return "Error: OCR model is still downloading. Please ensure you have internet access and try again later."
    }
    
    suspend fun extractTextFromBitmaps(bitmaps: List<Bitmap>): String {
        return try {
            val textBuilder = StringBuilder()
            for (bitmap in bitmaps) {
                val text = extractTextFromBitmap(bitmap)
                textBuilder.append(text).append("\n")
            }
            textBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text: ${e.message}"
        }
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
                    e.printStackTrace()
                    return@withContext "Error extracting text: ${e.message}"
                }
            }
        }
        return@withContext "Error: OCR model is still downloading. Please ensure you have internet access and try again later."
    }
}
