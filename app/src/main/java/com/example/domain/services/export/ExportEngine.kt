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

class ExportEngine(private val context: Context) {
    fun exportProjectFiles(
        projectName: String,
        htmlContent: String,
        onComplete: (pdfFile: File, htmlFile: File) -> Unit
    ) {
        try {
            val safeName = projectName.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            // 🔥 Target: storage/emulated/0/aipdfs/(project name)/pdf and html
            val baseDir = File(Environment.getExternalStorageDirectory(), "aipdfs/$safeName")
            val pdfDir = File(baseDir, "pdf")
            val htmlDir = File(baseDir, "html")
            pdfDir.mkdirs()
            htmlDir.mkdirs()

            val htmlFile = File(htmlDir, "${safeName}_notes.html")
            htmlFile.writeText(htmlContent)

            val pdfFile = File(pdfDir, "${safeName}_notes.pdf")

            Handler(Looper.getMainLooper()).post {
                try {
                    val webView = WebView(context)
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.javaScriptEnabled = false

                    var executed = false
                    val finishExport = {
                        if (!executed) {
                            executed = true
                            try {
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(800, 1200, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                webView.draw(page.canvas)
                                pdfDocument.finishPage(page)

                                val fos = FileOutputStream(pdfFile)
                                pdfDocument.writeTo(fos)
                                pdfDocument.close()
                                fos.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            Toast.makeText(context, "Saved to /storage/emulated/0/aipdfs/$safeName/", Toast.LENGTH_LONG).show()
                            onComplete(pdfFile, htmlFile)
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.postDelayed({ finishExport() }, 400)
                        }
                    }

                    val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY)
                    val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.EXACTLY)
                    webView.measure(widthMeasureSpec, heightMeasureSpec)
                    webView.layout(0, 0, 800, 1200)

                    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

                    webView.handler.postDelayed({ finishExport() }, 2000)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onComplete(pdfFile, htmlFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
