import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

# I will find "val totalPages =" and replace until "onComplete(pdfFile, htmlFile)"

start_str = "val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)"
end_str = "onComplete(pdfFile, htmlFile)"

start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

new_block = """val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                                
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
                                }"""

if start_idx != -1:
    content = content[:start_idx] + new_block + content[end_idx:]
    with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found")
