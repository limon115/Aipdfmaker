package com.example.ui.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

/**
 * Atmospheric glassmorphic background featuring:
 * 1. Multi-stop pastel base linear gradient (#D9EDFF -> #E1DEFF -> #FFE3EF -> #C8EFFF)
 * 2. Large soft radial light blooms (cyan, lavender, pink)
 * 3. Flowing translucent curved wave paths
 *
 * Automatically adapts between the light pastel palette and deep atmospheric dark palette.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark

    val bgGradient = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E1B4B),
                    Color(0xFF1E293B),
                    Color(0xFF0B1120)
                ),
                start = Offset.Zero,
                end = Offset(1000f, 1800f)
            )
        } else {
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
        }
    }

    val bloomCyan = remember(isDark) {
        if (isDark) Color(0xFF00E5FF).copy(alpha = 0.20f)
        else Color(0xFF79DFFF).copy(alpha = 0.42f)
    }
    val bloomLavender = remember(isDark) {
        if (isDark) Color(0xFF818CF8).copy(alpha = 0.22f)
        else Color(0xFFB9A4FF).copy(alpha = 0.32f)
    }
    val bloomPink = remember(isDark) {
        if (isDark) Color(0xFFE879F9).copy(alpha = 0.18f)
        else Color(0xFFFFBBD9).copy(alpha = 0.35f)
    }

    val wave1Color = remember(isDark) {
        Color.White.copy(alpha = if (isDark) 0.18f else 0.55f)
    }
    val wave2Color = remember(isDark) {
        Color.White.copy(alpha = if (isDark) 0.14f else 0.42f)
    }
    val wave3Color = remember(isDark) {
        Color.White.copy(alpha = if (isDark) 0.10f else 0.30f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Layer 2: Blurred light blooms
            // Soft cyan light bloom (top right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloomCyan,
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
                        bloomLavender,
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
                        bloomPink,
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
                color = wave1Color,
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
                color = wave2Color,
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
                color = wave3Color,
                style = Stroke(width = 1.0.dp.toPx())
            )
        }

        // Layer 4: Glass surfaces & Screen Content
        content()
    }
}
