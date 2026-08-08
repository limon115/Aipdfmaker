import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt'
with open(path, 'r') as f:
    content = f.read()

replacement = """
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                
                                val pageWidth = 800
                                val pageHeight = 1200
                                val contentHeight = webView.computeVerticalScrollRange()
                                val totalPages = Math.ceil(contentHeight.toDouble() / pageHeight).toInt().coerceAtLeast(1)
                                
                                for (i in 0 until totalPages) {
                                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                                    val page = pdfDocument.startPage(pageInfo)
                                    
                                    page.canvas.save()
                                    page.canvas.translate(0f, -(i * pageHeight).toFloat())
                                    webView.draw(page.canvas)
                                    page.canvas.restore()
                                    
                                    pdfDocument.finishPage(page)
                                }
"""

target = """
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(800, 1200, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                webView.draw(page.canvas)
                                pdfDocument.finishPage(page)
"""

content = content.replace(target.strip(), replacement.strip())

# Change layout to full height
layout_replacement = """
                    val contentHeightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                    webView.measure(widthMeasureSpec, contentHeightMeasureSpec)
                    val fullHeight = webView.measuredHeight
                    webView.layout(0, 0, 800, fullHeight.coerceAtLeast(1200))
"""
layout_target = """
                    webView.measure(widthMeasureSpec, heightMeasureSpec)
                    webView.layout(0, 0, 800, 1200)
"""

content = content.replace(layout_target.strip(), layout_replacement.strip())

with open(path, 'w') as f:
    f.write(content)
