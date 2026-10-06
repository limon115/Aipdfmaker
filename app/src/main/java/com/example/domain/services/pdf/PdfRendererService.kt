package com.example.domain.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.domain.services.ocr.LocalOcrEngine
import com.example.utils.AppLogger
import com.example.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererService(private val context: Context) {
    
    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            AppLogger.w("PdfRendererService", "PDFBox init warning: ${e.message}")
        }
    }

    /**
     * Extracts text from a PDF file using PDFBox text stripping with an automatic fallback to MLKit OCR via Android's native PdfRenderer for scanned/image PDFs.
     */
    suspend fun extractTextFromPdf(uri: Uri, ocrEngine: LocalOcrEngine): String = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        var pdfBoxText: String? = null
        
        try {
            // Copy URI input stream to temp cache file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Could not open input stream for PDF URI: $uri")

            // Try primary extraction via PDFBox
            try {
                PDDocument.load(cacheFile).use { document ->
                    val stripper = PDFTextStripper()
                    pdfBoxText = stripper.getText(document)
                }
            } catch (e: Exception) {
                AppLogger.w("PdfRendererService", "PDFBox extraction failed: ${e.message}, trying native OCR fallback")
            }

            // If PDFBox extracted valid non-whitespace text, return it
            if (!pdfBoxText.isNullOrBlank() && pdfBoxText!!.trim().length > 15) {
                return@withContext pdfBoxText!!.trim()
            }

            // Fallback: If PDF is scanned or image-heavy, render pages to Bitmaps and run OCR
            AppLogger.i("PdfRendererService", "PDFBox returned empty text or failed; running MLKit OCR page rendering fallback")
            val ocrText = renderPdfPagesForOcr(uri, ocrEngine)
            if (ocrText.isNotBlank() && !ocrText.startsWith("Error")) {
                return@withContext ocrText.trim()
            }

            // Return PDFBox text if available, or OCR text, or empty string warning
            val finalResult = pdfBoxText?.trim()?.ifBlank { null } ?: ocrText.trim().ifBlank { null }
            return@withContext finalResult ?: "No readable text found in PDF document."

        } catch (e: Exception) {
            AppLogger.e("PdfRendererService", "Error in extractTextFromPdf for URI $uri", e)
            return@withContext "Error extracting PDF text: ${e.message}"
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            FileUtils.cleanTempCache(context)
        }
    }

    private suspend fun renderPdfPagesForOcr(uri: Uri, ocrEngine: LocalOcrEngine, maxPages: Int = 15): String {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val pagesToRender = minOf(renderer.pageCount, maxPages)
                for (i in 0 until pagesToRender) {
                    val page = renderer.openPage(i)
                    // Render at 2x resolution for OCR quality
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
                renderer.close()
            }

            if (bitmaps.isNotEmpty()) {
                return ocrEngine.extractTextFromBitmaps(bitmaps)
            }
        } catch (e: Exception) {
            AppLogger.e("PdfRendererService", "Native PdfRenderer page rendering failed", e)
        } finally {
            bitmaps.forEach { 
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
        return ""
    }
}
