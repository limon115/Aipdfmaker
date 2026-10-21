package com.example.ui.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Atmospheric glassmorphic background featuring:
 * 1. Multi-stop pastel base linear gradient (#D9EDFF -> #E1DEFF -> #FFE3EF -> #C8EFFF)
 * 2. Large soft radial light blooms (cyan, lavender, pink)
 * 3. Flowing translucent curved wave paths
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD9EDFF),
                        Color(0xFFE1DEFF),
                        Color(0xFFFFE3EF),
                        Color(0xFFC8EFFF)
                    ),
                    start = Offset.Zero,
                    end = Offset(1000f, 1800f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Layer 2: Blurred light blooms
            // Soft cyan light bloom (top right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF79DFFF).copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.23f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.85f, h * 0.23f)
            )

            // Lavender light bloom (mid left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB9A4FF).copy(alpha = 0.32f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f, h * 0.48f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.18f, h * 0.48f)
            )

            // Pink light bloom (lower left / center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFBBD9).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * 0.78f),
                    radius = w * 0.62f
                ),
                radius = w * 0.62f,
                center = Offset(w * 0.22f, h * 0.78f)
            )

            // Layer 3: Translucent flowing wave lines
            val wave1 = Path().apply {
                moveTo(-w * 0.1f, h * 0.18f)
                cubicTo(
                    w * 0.20f, h * 0.08f,
                    w * 0.45f, h * 0.30f,
                    w * 0.68f, h * 0.18f
                )
                cubicTo(
                    w * 0.83f, h * 0.11f,
                    w * 1.05f, h * 0.14f,
                    w * 1.15f, h * 0.10f
                )
            }
            drawPath(
                path = wave1,
                color = Color.White.copy(alpha = 0.55f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            val wave2 = Path().apply {
                moveTo(-w * 0.08f, h * 0.32f)
                cubicTo(
                    w * 0.25f, h * 0.22f,
                    w * 0.52f, h * 0.40f,
                    w * 0.78f, h * 0.26f
                )
                cubicTo(
                    w * 0.92f, h * 0.18f,
                    w * 1.04f, h * 0.20f,
                    w * 1.15f, h * 0.16f
                )
            }
            drawPath(
                path = wave2,
                color = Color.White.copy(alpha = 0.42f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            val wave3 = Path().apply {
                moveTo(-w * 0.1f, h * 0.58f)
                cubicTo(
                    w * 0.22f, h * 0.48f,
                    w * 0.58f, h * 0.68f,
                    w * 0.85f, h * 0.54f
                )
                cubicTo(
                    w * 0.98f, h * 0.48f,
                    w * 1.08f, h * 0.50f,
                    w * 1.15f, h * 0.46f
                )
            }
            drawPath(
                path = wave3,
                color = Color.White.copy(alpha = 0.30f),
                style = Stroke(width = 1.0.dp.toPx())
            )
        }

        // Layer 4: Glass surfaces & Screen Content
        content()
    }
}
