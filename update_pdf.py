import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/pdf/PdfRendererService.kt', 'r') as f:
    content = f.read()

loop_old = """                    val pageCount = pdfRenderer.pageCount
                    for (i in 0 until pageCount) {
                        pdfRenderer.openPage(i).use { page ->
                            // Scale up the PDF page for better OCR accuracy (default is only 72 DPI)
                            val scale = 2.5f
                            val scaledWidth = (page.width * scale).toInt()
                            val scaledHeight = (page.height * scale).toInt()
                            
                            val bitmap = Bitmap.createBitmap(
                                scaledWidth, 
                                scaledHeight, 
                                Bitmap.Config.ARGB_8888
                            )
                            // PDFs often have transparent backgrounds, which can ruin OCR. Fill with white.
                            bitmap.eraseColor(Color.WHITE)
                            
                            val matrix = Matrix().apply { postScale(scale, scale) }
                            
                            page.render(
                                bitmap, 
                                null, 
                                matrix, 
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            
                            val text = ocrEngine.extractTextFromBitmap(bitmap)
                            textBuilder.append(text).append("\\n")
                            bitmap.recycle()
                        }
                    }"""

loop_new = """                    val pageCount = pdfRenderer.pageCount
                    var reusableBitmap: Bitmap? = null
                    var currentWidth = -1
                    var currentHeight = -1

                    for (i in 0 until pageCount) {
                        pdfRenderer.openPage(i).use { page ->
                            // Scale up the PDF page for better OCR accuracy (default is only 72 DPI)
                            // Reduced scale slightly to save memory and avoid Ashmem warnings on long PDFs
                            val scale = 2.0f
                            val scaledWidth = (page.width * scale).toInt()
                            val scaledHeight = (page.height * scale).toInt()
                            
                            val bitmap = if (reusableBitmap != null && currentWidth == scaledWidth && currentHeight == scaledHeight) {
                                reusableBitmap!!
                            } else {
                                reusableBitmap?.recycle()
                                currentWidth = scaledWidth
                                currentHeight = scaledHeight
                                val newBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                                reusableBitmap = newBitmap
                                newBitmap
                            }
                            
                            // PDFs often have transparent backgrounds, which can ruin OCR. Fill with white.
                            bitmap.eraseColor(Color.WHITE)
                            
                            val matrix = Matrix().apply { postScale(scale, scale) }
                            
                            page.render(
                                bitmap, 
                                null, 
                                matrix, 
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            
                            val text = ocrEngine.extractTextFromBitmap(bitmap)
                            textBuilder.append(text).append("\\n")
                            
                            // Yield to avoid blocking for too long on large PDFs
                            kotlinx.coroutines.yield()
                        }
                    }
                    reusableBitmap?.recycle()"""

if loop_old in content:
    content = content.replace(loop_old, loop_new)
    with open('/app/applet/app/src/main/java/com/example/domain/services/pdf/PdfRendererService.kt', 'w') as f:
        f.write(content)
    print("Updated PdfRendererService")
else:
    print("Could not find loop to replace")
