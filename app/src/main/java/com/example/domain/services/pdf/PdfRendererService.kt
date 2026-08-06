package com.example.domain.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererService(private val context: Context) {
    
    suspend fun renderPdfToBitmaps(uri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        
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
                            val bitmap = Bitmap.createBitmap(
                                page.width, 
                                page.height, 
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(
                                bitmap, 
                                null, 
                                null, 
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            bitmaps.add(bitmap)
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
        
        return@withContext bitmaps
    }
}
