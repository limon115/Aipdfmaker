import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

# Fix import
if "import kotlinx.coroutines.launch" not in content:
    content = content.replace("import java.io.FileOutputStream", "import java.io.FileOutputStream\nimport kotlinx.coroutines.GlobalScope\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\nimport kotlinx.coroutines.yield")

# Replace from line 57 to 105
old_snippet = """                            executed = true
                            try {
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                
                                val pageWidth = 800
                                val pageHeight = 1200
                                val contentHeight = webView.measuredHeight
                                val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                                
                                // Coroutine to prevent freezing UI on long documents
                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    try {
                                        for (i in 0 until totalPages) {
                                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                                            val page = pdfDocument.startPage(pageInfo)
                                            
                                            page.canvas.save()
                                            page.canvas.translate(0f, -(i * pageHeight).toFloat())
                                            webView.draw(page.canvas)
                                            page.canvas.restore()
                                            
                                            pdfDocument.finishPage(page)
                                            
                                            // Allow UI thread to breathe
                                            kotlinx.coroutines.yield()
                                        }
                                        
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            val fos = java.io.FileOutputStream(pdfFile)
                                            pdfDocument.writeTo(fos)
                                            pdfDocument.close()
                                            fos.close()
                                        }
                                        val displayPath = if (documentsDir.absolutePath.contains("Android/data")) {
                                            "Saved to App Files/Documents/aipdfs/$safeName/"
                                        } else {
                                            "Saved to Documents/aipdfs/$safeName/"
                                        }
                                        android.widget.Toast.makeText(context, displayPath, android.widget.Toast.LENGTH_LONG).show()
                                        onComplete(pdfFile, htmlFile)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        pdfDocument.close()
                                        android.widget.Toast.makeText(context, "PDF Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                        onComplete(pdfFile, htmlFile)
                                    }
                                }
                        }"""

new_snippet = """                            executed = true
                            val pdfDocument = android.graphics.pdf.PdfDocument()
                            val pageWidth = 800
                            val pageHeight = 1200
                            val contentHeight = webView.measuredHeight
                            val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                            
                            // Coroutine to prevent freezing UI on long documents
                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    for (i in 0 until totalPages) {
                                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                                        val page = pdfDocument.startPage(pageInfo)
                                        
                                        page.canvas.save()
                                        page.canvas.translate(0f, -(i * pageHeight).toFloat())
                                        webView.draw(page.canvas)
                                        page.canvas.restore()
                                        
                                        pdfDocument.finishPage(page)
                                        
                                        // Allow UI thread to breathe
                                        yield()
                                    }
                                    
                                    withContext(Dispatchers.IO) {
                                        val fos = java.io.FileOutputStream(pdfFile)
                                        pdfDocument.writeTo(fos)
                                        pdfDocument.close()
                                        fos.close()
                                    }
                                    val displayPath = if (documentsDir.absolutePath.contains("Android/data")) {
                                        "Saved to App Files/Documents/aipdfs/$safeName/"
                                    } else {
                                        "Saved to Documents/aipdfs/$safeName/"
                                    }
                                    android.widget.Toast.makeText(context, displayPath, android.widget.Toast.LENGTH_LONG).show()
                                    onComplete(pdfFile, htmlFile)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    pdfDocument.close()
                                    android.widget.Toast.makeText(context, "PDF Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    onComplete(pdfFile, htmlFile)
                                }
                            }
                        }"""

if old_snippet in content:
    content = content.replace(old_snippet, new_snippet)
    with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Fixed syntax")
else:
    print("Snippet not found")
