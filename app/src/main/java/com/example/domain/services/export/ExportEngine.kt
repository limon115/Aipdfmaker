package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
        try {
            val rootDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aipdfs")
            rootDir.mkdirs()

            val safeName = projectName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val htmlFile = File(rootDir, "${safeName}_notes.html")
            htmlFile.writeText(htmlContent)

            val pdfFile = File(rootDir, "${safeName}_notes.pdf")

            CoroutineScope(Dispatchers.Main).launch {
                val webView = WebView(context)
                webView.settings.loadsImagesAutomatically = true
                webView.settings.javaScriptEnabled = false

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.postDelayed({
                            try {
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(800, 1200, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                view.draw(page.canvas)
                                pdfDocument.finishPage(page)

                                val fos = FileOutputStream(pdfFile)
                                pdfDocument.writeTo(fos)
                                pdfDocument.close()
                                fos.close()

                                Toast.makeText(context, "Saved to Documents/aipdfs!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                onComplete(pdfFile, htmlFile)
                            }
                        }, 800)
                    }
                }

                val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY)
                val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.EXACTLY)
                webView.measure(widthMeasureSpec, heightMeasureSpec)
                webView.layout(0, 0, 800, 1200)

                webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
