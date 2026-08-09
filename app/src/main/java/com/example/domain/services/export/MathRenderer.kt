package com.example.domain.services.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

class MathLayout(
    val width: Float,
    val height: Float,
    val baseline: Float,
    val drawCommands: List<(Canvas, Float, Float) -> Unit>
)

interface MathRenderer {
    fun measure(latex: String, textSize: Float): MathLayout
    fun draw(canvas: Canvas, layout: MathLayout, x: Float, y: Float)
}

class BasicNativeMathRenderer(private val basePaint: Paint) : MathRenderer {

    override fun measure(latex: String, textSize: Float): MathLayout {
        val cleanLatex = latex
            .replace("\\frac{", "")
            .replace("}{", "/")
            .replace("}", "")
            .replace("\\times", "x")
            .replace("\\text{", "")
            .replace("\\sqrt{", "√")
            
        val paint = Paint(basePaint).apply {
            this.textSize = textSize
        }
        val width = paint.measureText(cleanLatex)
        val fontMetrics = paint.fontMetrics
        val height = fontMetrics.bottom - fontMetrics.top
        
        val commands = listOf<(Canvas, Float, Float) -> Unit> { canvas, x, y ->
            canvas.drawText(cleanLatex, x, y, paint)
        }
        
        return MathLayout(width, height, -fontMetrics.ascent, commands)
    }

    override fun draw(canvas: Canvas, layout: MathLayout, x: Float, y: Float) {
        for (cmd in layout.drawCommands) {
            cmd(canvas, x, y)
        }
    }
}
