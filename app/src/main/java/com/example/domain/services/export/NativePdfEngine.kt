package com.example.domain.services.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.models.document.*
import java.io.File
import java.io.FileOutputStream

data class PdfPageConfig(
    val width: Int = 595, // A4 width at 72 PPI
    val height: Int = 842, // A4 height at 72 PPI
    val marginLeft: Float = 50f,
    val marginTop: Float = 50f,
    val marginRight: Float = 50f,
    val marginBottom: Float = 50f
)

class NativePdfEngine(private val context: Context) {
    
    private val config = PdfPageConfig()
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    private val headingPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 18f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val mathRenderer: MathRenderer = BasicNativeMathRenderer(textPaint)

    fun exportDocumentToPdf(document: Document, pdfFile: File) {
        val pdfDocument = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(config.width, config.height, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var currentY = config.marginTop
        
        fun checkNewPage(requiredHeight: Float) {
            if (currentY + requiredHeight > config.height - config.marginBottom) {
                pdfDocument.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(config.width, config.height, 1).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = config.marginTop
            }
        }

        // Title
        if (document.title.isNotEmpty()) {
            val titlePaint = Paint(headingPaint).apply { textSize = 24f }
            val titleHeight = titlePaint.fontMetrics.descent - titlePaint.fontMetrics.ascent
            checkNewPage(titleHeight)
            canvas.drawText(document.title, config.marginLeft, currentY - titlePaint.fontMetrics.ascent, titlePaint)
            currentY += titleHeight + 20f
        }

        for (block in document.blocks) {
            when (block) {
                is HeadingBlock -> {
                    val paint = Paint(headingPaint).apply {
                        textSize = when (block.level) {
                            1 -> 18f
                            2 -> 16f
                            3 -> 14f
                            else -> 12f
                        }
                    }
                    val fm = paint.fontMetrics
                    val h = fm.descent - fm.ascent
                    checkNewPage(h)
                    canvas.drawText(block.text, config.marginLeft, currentY - fm.ascent, paint)
                    currentY += h + 10f
                }
                is ParagraphBlock -> {
                    // Very simple word wrap for paragraph
                    val text = block.text.ifEmpty { 
                        block.content?.joinToString("") { 
                            when (it) {
                                is TextElement -> it.value
                                is InlineMathElement -> it.latex
                            }
                        } ?: ""
                    }
                    
                    val words = text.split(" ")
                    var line = ""
                    val maxWidth = config.width - config.marginLeft - config.marginRight
                    val fm = textPaint.fontMetrics
                    val lineHeight = fm.descent - fm.ascent + 4f
                    
                    for (word in words) {
                        val testLine = if (line.isEmpty()) word else "$line $word"
                        if (textPaint.measureText(testLine) <= maxWidth) {
                            line = testLine
                        } else {
                            checkNewPage(lineHeight)
                            canvas.drawText(line, config.marginLeft, currentY - fm.ascent, textPaint)
                            currentY += lineHeight
                            line = word
                        }
                    }
                    if (line.isNotEmpty()) {
                        checkNewPage(lineHeight)
                        canvas.drawText(line, config.marginLeft, currentY - fm.ascent, textPaint)
                        currentY += lineHeight
                    }
                    currentY += 10f
                }
                is EquationBlock -> {
                    val layout = mathRenderer.measure(block.latex, 14f)
                    checkNewPage(layout.height)
                    val x = if (block.display) (config.width - layout.width) / 2f else config.marginLeft
                    mathRenderer.draw(canvas, layout, x, currentY + layout.baseline)
                    currentY += layout.height + 10f
                }
                is BulletListBlock -> {
                    val fm = textPaint.fontMetrics
                    val lineHeight = fm.descent - fm.ascent + 4f
                    for (item in block.items) {
                        checkNewPage(lineHeight)
                        canvas.drawText("• $item", config.marginLeft + 10f, currentY - fm.ascent, textPaint)
                        currentY += lineHeight
                    }
                    currentY += 10f
                }
                is NumberedListBlock -> {
                    val fm = textPaint.fontMetrics
                    val lineHeight = fm.descent - fm.ascent + 4f
                    for ((index, item) in block.items.withIndex()) {
                        checkNewPage(lineHeight)
                        canvas.drawText("${index + 1}. $item", config.marginLeft + 10f, currentY - fm.ascent, textPaint)
                        currentY += lineHeight
                    }
                    currentY += 10f
                }
                is TableBlock -> {
                    // Simplistic table rendering
                    val fm = textPaint.fontMetrics
                    val lineHeight = fm.descent - fm.ascent + 8f
                    val colWidth = (config.width - config.marginLeft - config.marginRight) / (block.columns.size.coerceAtLeast(1))
                    
                    // Draw header
                    checkNewPage(lineHeight)
                    for ((index, col) in block.columns.withIndex()) {
                        canvas.drawText(col, config.marginLeft + index * colWidth, currentY - fm.ascent + 4f, textPaint)
                    }
                    currentY += lineHeight
                    
                    // Draw rows
                    for (row in block.rows) {
                        checkNewPage(lineHeight)
                        for ((index, cell) in row.withIndex()) {
                            canvas.drawText(cell, config.marginLeft + index * colWidth, currentY - fm.ascent + 4f, textPaint)
                        }
                        currentY += lineHeight
                    }
                    currentY += 10f
                }
                is ImageBlock -> {
                    // Image skipped for now
                }
                is QuoteBlock -> {
                    val fm = textPaint.fontMetrics
                    val lineHeight = fm.descent - fm.ascent + 4f
                    checkNewPage(lineHeight)
                    val paint = Paint(textPaint).apply { typeface = Typeface.defaultFromStyle(Typeface.ITALIC) }
                    canvas.drawText(block.text, config.marginLeft + 20f, currentY - fm.ascent, paint)
                    currentY += lineHeight + 10f
                }
                is PageBreakBlock -> {
                    pdfDocument.finishPage(page)
                    pageInfo = PdfDocument.PageInfo.Builder(config.width, config.height, 1).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = config.marginTop
                }
            }
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(pdfFile))
        pdfDocument.close()
    }
}
