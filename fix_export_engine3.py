import sys
import re

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("webView.settings.javaScriptEnabled = false", "webView.settings.javaScriptEnabled = true")

target = """                        override fun onPageFinished(view: WebView, url: String) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    // Measure and layout the WebView to prevent PDF cutoff"""

replacement = """                        private var maxPolls = 10
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
                                    // Measure and layout the WebView to prevent PDF cutoff"""

if target in content:
    content = content.replace(target, replacement)
else:
    print("Could not find target block 1")

target2 = """                                        null
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Print Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    onComplete(null, htmlFile)
                                }
                            }, 500)
                        }
                    }"""

replacement2 = """                                        null
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Print Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    onComplete(null, htmlFile)
                                }
                        }
                    }"""

if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Could not find target block 2")

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
    f.write(content)
print("Done")
