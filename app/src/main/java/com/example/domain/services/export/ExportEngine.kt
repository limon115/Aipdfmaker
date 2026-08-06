package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ExportEngine(private val context: Context) {

    fun exportProjectFiles(
        projectName: String,
        htmlContent: String,
        onComplete: (pdfFile: File, htmlFile: File) -> Unit
    ) {
        val rootDir = File(Environment.getExternalStorageDirectory(), "aipdfs/$projectName")
        rootDir.mkdirs()

        val htmlFile = File(rootDir, "${projectName}_notes.html")
        htmlFile.writeText(htmlContent)

        val pdfFile = File(rootDir, "${projectName}_notes.pdf")

        CoroutineScope(Dispatchers.Main).launch {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    view.postDelayed({
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val width = view.measuredWidth.takeIf { it > 0 } ?: 800
                        val height = view.measuredHeight.takeIf { it > 0 } ?: 1200
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        
                        view.draw(page.canvas)
                        pdfDocument.finishPage(page)
                        
                        try {
                            pdfDocument.writeTo(FileOutputStream(pdfFile))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pdfDocument.close()
                        }
                        
                        onComplete(pdfFile, htmlFile)
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
}
