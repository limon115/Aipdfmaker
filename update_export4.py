import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

new_content = """package com.example.domain.services.export

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
                    webView.settings.javaScriptEnabled = false
                    
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = webView.createPrintDocumentAdapter(safeName)
                                    val jobName = "${context.getString(com.example.R.string.app_name)} Document - $safeName"
                                    
                                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                                    
                                    onComplete(null, htmlFile)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Print Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    onComplete(null, htmlFile)
                                }
                            }, 500)
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
"""

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
    f.write(new_content)

print("Updated ExportEngine to use PrintManager!")
