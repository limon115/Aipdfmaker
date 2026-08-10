package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.domain.models.document.Document
import com.example.domain.services.html.JsonToHtmlConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

class ExportEngine(private val context: Context) {
    fun exportProjectFiles(
        projectName: String,
        jsonContent: String,
        isPdf: Boolean = true,
        onComplete: (pdfFile: File?, jsonFile: File) -> Unit
    ) {
        try {
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }

            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")

            if (!baseDir.exists() && !baseDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
            }
            baseDir.mkdirs()

            val jsonFile = File(baseDir, "document.json")
            jsonFile.writeText(jsonContent)

            val htmlFile = File(baseDir, "document.html")

            try {
                val jsonFormat = Json { ignoreUnknownKeys = true; classDiscriminator = "type"; isLenient = true }
                val document = jsonFormat.decodeFromString<Document>(jsonContent)

                val htmlConverter = JsonToHtmlConverter()
                val htmlString = htmlConverter.convert(document)
                htmlFile.writeText(htmlString)

                if (isPdf) {
                    Handler(Looper.getMainLooper()).post {
                        val webView = WebView(context)
                        webView.settings.javaScriptEnabled = true
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                val handler = Handler(Looper.getMainLooper())
                                val timeoutMillis = 8000L
                                val intervalMillis = 150L
                                val startTime = System.currentTimeMillis()

                                val checkRunnable = object : Runnable {
                                    override fun run() {
                                        view.evaluateJavascript("document.body.getAttribute('data-render-complete');") { result ->
                                            if (result == "\"true\"" || result == "'true'" || result == "true") {
                                                printWebView(webView, safeName)
                                                onComplete(null, htmlFile)
                                            } else {
                                                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                                                    printWebView(webView, safeName)
                                                    onComplete(null, htmlFile)
                                                } else {
                                                    handler.postDelayed(this, intervalMillis)
                                                }
                                            }
                                        }
                                    }
                                }
                                handler.postDelayed(checkRunnable, intervalMillis)
                            }
                        }
                        webView.loadDataWithBaseURL("file:///android_asset/", htmlString, "text/html", "UTF-8", null)
                    }
                } else {
                    onComplete(null, htmlFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onComplete(null, jsonFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun printWebView(webView: WebView, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val originalAdapter = webView.createPrintDocumentAdapter(jobName)
        
        val wrappedAdapter = object : PrintDocumentAdapter() {
            override fun onStart() {
                originalAdapter.onStart()
            }

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                originalAdapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                originalAdapter.onWrite(pages, destination, cancellationSignal, callback)
            }

            override fun onFinish() {
                originalAdapter.onFinish()
                Handler(Looper.getMainLooper()).post {
                    webView.destroy()
                }
            }
        }

        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        printManager.print(jobName, wrappedAdapter, builder.build())
    }
}
