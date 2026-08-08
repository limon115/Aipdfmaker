package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class ExportEngine(private val context: Context) {
    fun exportProjectFiles(
        projectName: String,
        htmlContent: String,
        onComplete: (pdfFile: File, htmlFile: File) -> Unit
    ) {
        try {
            // 🔥 Strictly trim and sanitize project name to eliminate rogue spaces and special characters
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            // Map safely to /storage/emulated/0/Documents/aipdfs/(project)/
            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")
            var pdfDir = File(baseDir, "pdf")
            var htmlDir = File(baseDir, "html")
            
            if (!pdfDir.exists() && !pdfDir.mkdirs()) {
                // Fallback to app-specific external or internal directory if permissions fail
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
                pdfDir = File(baseDir, "pdf")
                htmlDir = File(baseDir, "html")
            }
            
            pdfDir.mkdirs()
            htmlDir.mkdirs()

            val htmlFile = File(htmlDir, "${safeName}_notes.html")
            htmlFile.writeText(htmlContent)

            val pdfFile = File(pdfDir, "${safeName}_notes.pdf")

            Handler(Looper.getMainLooper()).post {
                try {
                    WebView.enableSlowWholeDocumentDraw()
                    val webView = WebView(context)
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.javaScriptEnabled = false
                    webView.settings.textZoom = 100
                    webView.setInitialScale(100)

                    var executed = false
                    val finishExport = {
                        if (!executed) {
                            executed = true
                            val pdfDocument = android.graphics.pdf.PdfDocument()
                            val pageWidth = 800
                            val pageHeight = 1200
                            val contentHeight = webView.measuredHeight
                            val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                            
                            // Coroutine to prevent freezing UI on long documents
                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    for (i in 0 until totalPages) {
                                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                                        val page = pdfDocument.startPage(pageInfo)
                                        
                                        page.canvas.save()
                                        page.canvas.translate(0f, -(i * pageHeight).toFloat())
                                        webView.draw(page.canvas)
                                        page.canvas.restore()
                                        
                                        pdfDocument.finishPage(page)
                                        
                                        // Allow UI thread to breathe
                                        yield()
                                    }
                                    
                                    withContext(Dispatchers.IO) {
                                        val fos = java.io.FileOutputStream(pdfFile)
                                        pdfDocument.writeTo(fos)
                                        pdfDocument.close()
                                        fos.close()
                                    }
                                    val displayPath = if (documentsDir.absolutePath.contains("Android/data")) {
                                        "Saved to App Files/Documents/aipdfs/$safeName/"
                                    } else {
                                        "Saved to Documents/aipdfs/$safeName/"
                                    }
                                    android.widget.Toast.makeText(context, displayPath, android.widget.Toast.LENGTH_LONG).show()
                                    onComplete(pdfFile, htmlFile)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    pdfDocument.close()
                                    android.widget.Toast.makeText(context, "PDF Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    onComplete(pdfFile, htmlFile)
                                }
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            Handler(Looper.getMainLooper()).postDelayed({ finishExport() }, 400)
                        }
                    }

                    val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY)
                    val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.EXACTLY)
                    val contentHeightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                    webView.measure(widthMeasureSpec, contentHeightMeasureSpec)
                    val fullHeight = webView.measuredHeight
                    webView.layout(0, 0, 800, fullHeight.coerceAtLeast(1200))

                    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
                    Handler(Looper.getMainLooper()).postDelayed({ finishExport() }, 2000)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onComplete(pdfFile, htmlFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
