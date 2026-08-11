package com.example.domain.services.pdf

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfRendererService(private val context: Context) {
    
    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractTextFromPdf(uri: Uri, ocrEngine: com.example.domain.services.ocr.LocalOcrEngine): String = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            val document = PDDocument.load(cacheFile)
            val textStripper = PDFTextStripper()
            val text = textStripper.getText(document)
            document.close()
            
            return@withContext text
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error extracting text: ${e.message}"
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }
}
