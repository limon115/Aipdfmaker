with open("app/src/main/java/com/example/domain/services/export/ExportEngine.kt", "r") as f:
    text = f.read()

text = text.replace(
    "jsonContent: String,",
    "jsonContent: String,\n        isPdf: Boolean = true,"
)

target = """                // Render PDF using WebView
                Handler(Looper.getMainLooper()).post {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
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
                }"""

replacement = """                if (isPdf) {
                    // Render PDF using WebView
                    Handler(Looper.getMainLooper()).post {
                        val webView = WebView(context)
                        webView.settings.javaScriptEnabled = true
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
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
                }"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/domain/services/export/ExportEngine.kt", "w") as f:
    f.write(text)
