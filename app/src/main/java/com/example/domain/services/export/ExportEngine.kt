package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.domain.models.document.Document
import com.example.domain.services.html.JsonToHtmlConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExportEngine(private val context: Context) {
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    fun exportProjectFiles(
        projectName: String,
        jsonContent: String,
        isPdf: Boolean = true,
        onComplete: (pdfFile: File?, jsonFile: File) -> Unit
    ) {
        _exportProgress.value = 0f
        try {
            Timber.i("Exporting project %s as %s", projectName, if (isPdf) "PDF" else "HTML")
            com.example.utils.AppLogger.i("ExportEngine", "Exporting project $projectName as ${if (isPdf) "PDF" else "HTML"}")
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")
            Timber.d("Target base directory: %s", baseDir.absolutePath)
            
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
            }
            baseDir.mkdirs()
            
            val jsonFile = File(baseDir, "document.json")
            jsonFile.writeText(jsonContent)
            
            val htmlFile = File(baseDir, "document.html")
            
            _exportProgress.value = 0f
        try {
                // Parse JSON to Document
                val jsonFormat = Json { ignoreUnknownKeys = true; classDiscriminator = "type"; isLenient = true }
                val document = jsonFormat.decodeFromString<Document>(jsonContent)
                
                // Convert to HTML
                val htmlConverter = JsonToHtmlConverter()
                Timber.d("Starting HTML conversion for document: %s", document.title)
                val htmlString = htmlConverter.convert(document)
                Timber.d("HTML conversion complete. Length: %d", htmlString.length)
                htmlFile.writeText(htmlString)
                
                if (isPdf) {
                    // Render PDF using WebView
                    Handler(Looper.getMainLooper()).post {
                        val webView = WebView(context)
                        webView.settings.javaScriptEnabled = true
                        webView.webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                _exportProgress.value = newProgress / 100f
                            }
                        }
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                Timber.d("WebView onPageFinished triggered for URL: %s", url)
                                // Give KaTeX a moment to render
                                Handler(Looper.getMainLooper()).postDelayed({
                                    printWebView(webView, safeName)
                                    // We return null for pdfFile because PrintManager handles the PDF generation and saving UI
                                    onComplete(null, htmlFile)
                                }, 1000)
                            }
                        }
                        // Load the HTML string with a dummy base URL to allow KaTeX CDN to load
                        webView.loadDataWithBaseURL("https://example.com", htmlString, "text/html", "UTF-8", null)
                    }
                } else {
                    onComplete(null, htmlFile)
                }
            } catch (e: Exception) {
                Timber.e(e, "Render Error during export")
                com.example.utils.AppLogger.e("ExportEngine", "Render Error: ${e.localizedMessage}", e)
                e.printStackTrace()
                Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onComplete(null, jsonFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to export project")
            com.example.utils.AppLogger.e("ExportEngine", "Failed: ${e.localizedMessage}", e)
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun printWebView(webView: WebView, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        Timber.d("Initiating PrintManager.print for job: %s", jobName)
        printManager.print(jobName, printAdapter, builder.build())
    }
}
