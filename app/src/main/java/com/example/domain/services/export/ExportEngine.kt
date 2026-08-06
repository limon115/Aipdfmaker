package com.example.domain.services.export

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExportEngine(private val context: Context) {

    suspend fun saveAsHtml(htmlContent: String, fileName: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "${fileName}.html")
        file.writeText(htmlContent)
        file
    }

    fun generatePdfFromHtml(htmlContent: String, fileName: String, onComplete: (File) -> Unit) {
        val webView = WebView(context)
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Wait a moment for rendering
                view.postDelayed({
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val width = view.measuredWidth.takeIf { it > 0 } ?: 800
                    val height = view.measuredHeight.takeIf { it > 0 } ?: 1200
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    
                    view.draw(page.canvas)
                    pdfDocument.finishPage(page)
                    
                    val pdfFile = File(context.cacheDir, "${fileName}.pdf")
                    try {
                        pdfDocument.writeTo(java.io.FileOutputStream(pdfFile))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pdfDocument.close()
                    }
                    onComplete(pdfFile)
                }, 500)
            }
        }
        
        // Measure and layout so it draws
        val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.EXACTLY)
        webView.measure(widthMeasureSpec, heightMeasureSpec)
        webView.layout(0, 0, 800, 1200)
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}
