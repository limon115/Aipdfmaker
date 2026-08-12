import os

filepath = "app/src/main/java/com/example/domain/services/export/ExportEngine.kt"
with open(filepath, 'r') as f:
    code = f.read()

# 1. Inject WebView Security Bypass
old_webview_settings = """                        webView.settings.javaScriptEnabled = true
                        webView.settings.allowFileAccess = true
                        webView.settings.domStorageEnabled = true"""

new_webview_settings = """                        webView.settings.javaScriptEnabled = true
                        webView.settings.allowFileAccess = true
                        webView.settings.allowFileAccessFromFileURLs = true // Required for local KaTeX
                        webView.settings.allowUniversalAccessFromFileURLs = true // Required for local KaTeX
                        webView.settings.domStorageEnabled = true"""
code = code.replace(old_webview_settings, new_webview_settings)


# 2. Replace blind delay with Smart Polling
old_delay = """                                // Give KaTeX a moment to render
                                Handler(Looper.getMainLooper()).postDelayed({
                                    printWebView(webView, safeName)
                                    // We return null for pdfFile because PrintManager handles the PDF generation and saving UI
                                    onComplete(null, htmlFile)
                                }, 1000)"""

new_delay = """                                // 🛡️ Smart Polling: Wait for KaTeX 'data-render-complete'
                                pollForKaTeX(webView, safeName, htmlFile, onComplete, 0)"""
code = code.replace(old_delay, new_delay)


# 3. Inject the Polling Function at the bottom of the class
poll_function = """    private fun pollForKaTeX(webView: WebView, jobName: String, htmlFile: File, onComplete: (File?, File) -> Unit, attempts: Int) {
        if (attempts > 30) { // Timeout after 4.5 seconds
            Timber.e("KaTeX rendering timed out. Printing anyway.")
            printWebView(webView, jobName)
            onComplete(null, htmlFile)
            return
        }
        
        webView.evaluateJavascript("document.body.getAttribute('data-render-complete');") { result ->
            if (result != null && result.contains("true")) {
                Timber.d("KaTeX rendering verified complete. Triggering print.")
                printWebView(webView, jobName)
                onComplete(null, htmlFile)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    pollForKaTeX(webView, jobName, htmlFile, onComplete, attempts + 1)
                }, 150)
            }
        }
    }

}"""
# Replace the final closing brace with the new function
code = code.rsplit("}", 1)[0] + poll_function

with open(filepath, 'w') as f:
    f.write(code)
print("✅ ExportEngine patched: Security limits bypassed and smart polling injected.")
