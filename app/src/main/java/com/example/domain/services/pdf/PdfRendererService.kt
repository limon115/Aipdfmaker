package com.example.domain.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererService(private val context: Context) {
    
    suspend fun extractTextFromPdf(uri: Uri, ocrEngine: com.example.domain.services.ocr.LocalOcrEngine): String = withContext(Dispatchers.IO) {
        val textBuilder = StringBuilder()
        
        // We need to copy the content of the URI to a local cache file because 
        // PdfRenderer requires a FileDescriptor which is easier to get from a local file.
        val cacheFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            val fileDescriptor = android.os.ParcelFileDescriptor.open(
                cacheFile, 
                android.os.ParcelFileDescriptor.MODE_READ_ONLY
            )
            
            fileDescriptor.use { fd ->
                val pdfRenderer = PdfRenderer(fd)
                try {
                    val pageCount = pdfRenderer.pageCount
                    for (i in 0 until pageCount) {
                        pdfRenderer.openPage(i).use { page ->
                            // Scale up the PDF page for better OCR accuracy (default is only 72 DPI)
                            val scale = 2.5f
                            val scaledWidth = (page.width * scale).toInt()
                            val scaledHeight = (page.height * scale).toInt()
                            
                            val bitmap = Bitmap.createBitmap(
                                scaledWidth, 
                                scaledHeight, 
                                Bitmap.Config.ARGB_8888
                            )
                            // PDFs often have transparent backgrounds, which can ruin OCR. Fill with white.
                            bitmap.eraseColor(Color.WHITE)
                            
                            val matrix = Matrix().apply { postScale(scale, scale) }
                            
                            page.render(
                                bitmap, 
                                null, 
                                matrix, 
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            
                            val text = ocrEngine.extractTextFromBitmap(bitmap)
                            textBuilder.append(text).append("\n")
                            bitmap.recycle()
                        }
                    }
                } finally {
                    pdfRenderer.close()
                }
            }
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
        
        return@withContext textBuilder.toString()
    }
}
