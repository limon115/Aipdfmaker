import sys

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

target = """                        override fun onPageFinished(view: WebView, url: String) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = webView.createPrintDocumentAdapter(safeName)
                                    val jobName = "${context.getString(com.example.R.string.app_name)} Document - $safeName"
                                    
                                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                                    
                                    onComplete(null, htmlFile)
                                } catch (e: Exception) {"""

replacement = """                        override fun onPageFinished(view: WebView, url: String) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    // Measure and layout the WebView to prevent PDF cutoff
                                    val dm = context.resources.displayMetrics
                                    val width = dm.widthPixels.coerceAtLeast(1000)
                                    val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY)
                                    val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                                    
                                    view.measure(widthMeasureSpec, heightMeasureSpec)
                                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = webView.createPrintDocumentAdapter(safeName)
                                    val jobName = "${context.getString(com.example.R.string.app_name)} Document - $safeName"
                                    
                                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                                    
                                    onComplete(null, htmlFile)
                                } catch (e: Exception) {"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
