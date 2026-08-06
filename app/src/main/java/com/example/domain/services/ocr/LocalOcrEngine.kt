package com.example.domain.services.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class LocalOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text: ${e.message}"
        }
    }
    
    suspend fun extractTextFromBitmaps(bitmaps: List<Bitmap>): String {
        return try {
            val textBuilder = StringBuilder()
            for (bitmap in bitmaps) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                textBuilder.append(result.text).append("\n")
            }
            textBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text: ${e.message}"
        }
    }
}
