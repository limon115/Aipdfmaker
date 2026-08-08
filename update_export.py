import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

loop_old = """                                val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                                
                                for (i in 0 until totalPages) {
                                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                                    val page = pdfDocument.startPage(pageInfo)
                                    
                                    page.canvas.save()
                                    page.canvas.translate(0f, -(i * pageHeight).toFloat())
                                    webView.draw(page.canvas)
                                    page.canvas.restore()
                                    
                                    pdfDocument.finishPage(page)
                                }
                                val fos = FileOutputStream(pdfFile)
                                pdfDocument.writeTo(fos)
                                pdfDocument.close()
                                fos.close()"""

loop_new = """                                val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                                
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
                                            val fos = FileOutputStream(pdfFile)
                                            pdfDocument.writeTo(fos)
                                            pdfDocument.close()
                                            fos.close()
                                        }
                                        val displayPath = if (documentsDir.absolutePath.contains("Android/data")) {
                                            "Saved to App Files/Documents/aipdfs/$safeName/"
                                        } else {
                                            "Saved to Documents/aipdfs/$safeName/"
                                        }
                                        Toast.makeText(context, displayPath, Toast.LENGTH_LONG).show()
                                        onComplete(pdfFile, htmlFile)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        pdfDocument.close()
                                        Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        onComplete(pdfFile, htmlFile)
                                    }
                                }
                                // Return early so we don't hit the synchronous file save / onComplete below
                                return@try"""

if loop_old in content:
    content = content.replace(loop_old, loop_new)
    
    # We also need to remove the synchronous onComplete(pdfFile, htmlFile) below the old loop
    # because now we do it in the coroutine.
    sync_onComplete_old = """                                Toast.makeText(context, displayPath, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                            onComplete(pdfFile, htmlFile)"""
    
    sync_onComplete_new = """                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                onComplete(pdfFile, htmlFile)
                            }"""
    
    content = content.replace(sync_onComplete_old, sync_onComplete_new)

    with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Updated ExportEngine")
else:
    print("Could not find loop to replace")
