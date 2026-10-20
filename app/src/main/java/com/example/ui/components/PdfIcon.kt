package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun PdfIcon(modifier: Modifier = Modifier) {
    val isDark = AppTheme.colors.isDark
    // Light grayish-white background
    val bgColor = if (isDark) Color(0xFF232734) else Color(0xFFF1F3F5)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Document dimensions: crisp centered dog-eared file
            val docW = w * 0.54f
            val docH = h * 0.70f
            val left = (w - docW) / 2f
            val top = (h - docH) / 2f
            val right = left + docW
            val bottom = top + docH
            val cr = docW * 0.12f // rounded bottom corners and top-left
            val fold = docW * 0.35f // dog-ear fold width/height

            val redColor = Color(0xFFE53935)     // Solid red file body
            val foldColor = Color(0xFFB71C1C)    // Darker red for folded flap

            // 1. Solid Red File Body
            val docPath = Path().apply {
                moveTo(left + cr, top)
                lineTo(right - fold, top)
                lineTo(right, top + fold)
                lineTo(right, bottom - cr)
                arcTo(Rect(right - cr * 2, bottom - cr * 2, right, bottom), 0f, 90f, false)
                lineTo(left + cr, bottom)
                arcTo(Rect(left, bottom - cr * 2, left + cr * 2, bottom), 90f, 90f, false)
                lineTo(left, top + cr)
                arcTo(Rect(left, top, left + cr * 2, top + cr * 2), 180f, 90f, false)
                close()
            }
            drawPath(path = docPath, color = redColor, style = Fill)

            // 2. Dog-eared folded corner at top-right
            val foldPath = Path().apply {
                moveTo(right - fold, top)
                lineTo(right - fold, top + fold)
                lineTo(right, top + fold)
                close()
            }
            drawPath(path = foldPath, color = foldColor, style = Fill)

            // 3. Document text lines inside the solid red file
            val lineWidth = docW * 0.55f
            val lineX = left + (docW - lineWidth) / 2f
            val lineH = docH * 0.06f
            val line1Y = top + (docH * 0.46f)
            val line2Y = top + (docH * 0.60f)
            val line3Y = top + (docH * 0.74f)

            // Three clean white document lines
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(lineX, line1Y),
                size = Size(lineWidth, lineH),
                cornerRadius = CornerRadius(lineH / 2f, lineH / 2f)
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(lineX, line2Y),
                size = Size(lineWidth, lineH),
                cornerRadius = CornerRadius(lineH / 2f, lineH / 2f)
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(lineX, line3Y),
                size = Size(lineWidth * 0.65f, lineH),
                cornerRadius = CornerRadius(lineH / 2f, lineH / 2f)
            )
        }
    }
}
