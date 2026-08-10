import os

filepath = "app/src/main/java/com/example/domain/services/export/ExportEngine.kt"
with open(filepath, "r") as f:
    text = f.read()

# Add imports for StateFlow
import_string = """import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
"""

text = text.replace('import timber.log.Timber\n', 'import timber.log.Timber\n' + import_string)

# Add progress state variable
state_var_string = """class ExportEngine(private val context: Context) {
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

"""
text = text.replace('class ExportEngine(private val context: Context) {\n', state_var_string)

# Update the exportProjectFiles to reset progress
text = text.replace('        try {\n', '        _exportProgress.value = 0f\n        try {\n')

# Add WebChromeClient to webView
web_chrome_client_string = """                        webView.settings.javaScriptEnabled = true
                        webView.webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                _exportProgress.value = newProgress / 100f
                            }
                        }"""
text = text.replace('                        webView.settings.javaScriptEnabled = true', web_chrome_client_string)

with open(filepath, "w") as f:
    f.write(text)
