package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.File

class ExportEngine(private val context: Context) {
    fun exportProjectFiles(
        projectName: String,
        htmlContent: String,
        onComplete: (pdfFile: File?, htmlFile: File) -> Unit
    ) {
        try {
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")
            var htmlDir = File(baseDir, "html")
            
            if (!htmlDir.exists() && !htmlDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
                htmlDir = File(baseDir, "html")
            }
            htmlDir.mkdirs()
            
            val htmlFile = File(htmlDir, "${safeName}_notes.html")
            htmlFile.writeText(htmlContent)
            
            Handler(Looper.getMainLooper()).post {
                try {
                    val webView = WebView(context)
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.javaScriptEnabled = true
                    
                    webView.webViewClient = object : WebViewClient() {
                        private var maxPolls = 10
                        private var lastHeight = -1
                        private var currentPoll = 0
                        
                        override fun onPageFinished(view: WebView, url: String) {
                            checkHeightAndPrint(view)
                        }
                        
                        private fun checkHeightAndPrint(view: WebView) {
                            view.evaluateJavascript("Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)") { result ->
                                val height = result?.replace("\"", "")?.toIntOrNull() ?: 0
                                if (height > 0 && height == lastHeight) {
                                    doPrint(view)
                                } else if (currentPoll >= maxPolls) {
                                    doPrint(view)
                                } else {
                                    lastHeight = height
                                    currentPoll++
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        checkHeightAndPrint(view)
                                    }, 200)
                                }
                            }
                        }
                        
                        private fun doPrint(view: WebView) {
                                try {
                                    // Measure and layout the WebView to prevent PDF cutoff
                                    val dm = context.resources.displayMetrics
                                    val width = dm.widthPixels.coerceAtLeast(1000)
                                    val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY)
                                    val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                                    
                                    view.measure(widthMeasureSpec, heightMeasureSpec)
                                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = view.createPrintDocumentAdapter(safeName)
                                    val jobName = "${context.getString(com.example.R.string.app_name)} Document - $safeName"
                                    
                                    val attributes = android.print.PrintAttributes.Builder()
                                        .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                        .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                        .setResolution(android.print.PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                                        .build()
                                        
                                    printManager.print(jobName, printAdapter, attributes)
                                    
                                    onComplete(null, htmlFile)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Print Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    onComplete(null, htmlFile)
                                }
                        }
                    }
                    
                    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onComplete(null, htmlFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
