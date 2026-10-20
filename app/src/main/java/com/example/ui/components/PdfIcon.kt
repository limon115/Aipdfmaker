package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser

@Composable
fun PdfIcon(modifier: Modifier = Modifier) {
    // The official Adobe Acrobat PDF knot logo
    val knotPathStr = "M23.63 15.3c-.71-.745-2.166-1.17-4.224-1.17-1.1 0-2.377.106-3.761.354a19.443 19.443 0 0 1-2.307-2.661c-.532-.71-.994-1.49-1.42-2.236.817-2.484 1.207-4.507 1.207-5.962 0-1.632-.603-3.336-2.342-3.336-.532 0-1.065.32-1.349.781-.78 1.384-.425 4.4.923 7.381a60.277 60.277 0 0 1-1.703 4.507c-.568 1.349-1.207 2.733-1.917 4.01C2.834 18.53.314 20.34.03 21.758c-.106.533.071 1.03.462 1.42.142.107.639.533 1.49.533 2.59 0 5.323-4.188 6.707-6.707 1.065-.355 2.13-.71 3.194-.994a34.963 34.963 0 0 1 3.407-.745c2.732 2.448 5.145 2.839 6.352 2.839 1.49 0 2.023-.604 2.2-1.1.32-.64.106-1.349-.213-1.704zm-1.42 1.03c-.107.532-.64.887-1.384.887-.213 0-.39-.036-.604-.071-1.348-.32-2.626-.994-3.903-2.059a17.717 17.717 0 0 1 2.98-.248c.746 0 1.385.035 1.81.142.497.106 1.278.426 1.1 1.348zm-7.524-1.668a38.01 38.01 0 0 0-2.945.674 39.68 39.68 0 0 0-2.52.745 40.05 40.05 0 0 0 1.207-2.555c.426-.994.78-2.023 1.136-2.981.354.603.745 1.207 1.135 1.739a50.127 50.127 0 0 0 1.987 2.378zM10.038 1.46a.768.768 0 0 1 .674-.425c.745 0 .887.851.887 1.526 0 1.135-.355 2.874-.958 4.861-1.03-2.768-1.1-5.074-.603-5.962zM6.134 17.997c-1.81 2.981-3.549 4.826-4.613 4.826a.872.872 0 0 1-.532-.177c-.213-.213-.32-.461-.249-.745.213-1.065 2.271-2.555 5.394-3.904Z"
    
    val knotPath = remember { PathParser().parsePathString(knotPathStr).toPath() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22))
            .background(Color(0xFFFDE8E9))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Document bounds
            val docW = w * 0.55f
            val docH = h * 0.65f
            val left = (w - docW) / 2f
            val top = (h - docH) / 2f
            val right = left + docW
            val bottom = top + docH
            val cr = docW * 0.15f // corner radius
            val fold = docW * 0.35f // fold size
            
            // Draw red document shape
            val docPath = Path().apply {
                moveTo(left + cr, top)
                lineTo(right - fold, top)
                // Straight cut for the fold base
                lineTo(right, top + fold)
                lineTo(right, bottom - cr)
                arcTo(Rect(right - cr * 2, bottom - cr * 2, right, bottom), 0f, 90f, false)
                lineTo(left + cr, bottom)
                arcTo(Rect(left, bottom - cr * 2, left + cr * 2, bottom), 90f, 90f, false)
                lineTo(left, top + cr)
                arcTo(Rect(left, top, left + cr * 2, top + cr * 2), 180f, 90f, false)
                close()
            }
            drawPath(path = docPath, color = Color(0xFFDF3032), style = Fill)
            
            // Draw dark blue folded corner
            val foldPath = Path().apply {
                moveTo(right - fold, top)
                lineTo(right, top + fold)
                // Curved inner edge of the fold
                quadraticBezierTo(
                    right - fold * 1.1f, top + fold * 1.1f,
                    right - fold, top
                )
                close()
            }
            drawPath(path = foldPath, color = Color(0xFF383A53), style = Fill)
            
            // The SVG is natively 24x24. Scale it to fit nicely inside the document.
            // We want it to be about 40% of the document width.
            val targetSize = docW * 0.45f
            val scaleRatio = targetSize / 24f
            
            // Center the knot visually
            val dx = left + (docW - targetSize) / 2f
            val dy = top + (docH - targetSize) / 2f + (docH * 0.05f) // push down slightly
            
            translate(left = dx, top = dy) {
                scale(scale = scaleRatio, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(path = knotPath, color = Color.White, style = Fill)
                }
            }
        }
    }
}
