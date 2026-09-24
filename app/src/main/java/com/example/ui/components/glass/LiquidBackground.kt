package com.example.ui.components.glass

import androidx.compose.animation.core.*
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
import com.example.ui.theme.AppTheme

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    val isDark = colors.isDark

    // Modern deep background canvas color
    val bgColor = if (isDark) Color(0xFF0B0D14) else Color(0xFFF1F3F9)
    
    // Liquid ambient palette
    val color1 = Color(0xFF6C63FF).copy(alpha = if (isDark) 0.38f else 0.55f) // Electric Purple
    val color2 = Color(0xFF00E5FF).copy(alpha = if (isDark) 0.30f else 0.45f) // Cyan Glow
    val color3 = Color(0xFFFF5252).copy(alpha = if (isDark) 0.22f else 0.35f) // Coral Pink
    val color4 = Color(0xFF7C4DFF).copy(alpha = if (isDark) 0.28f else 0.42f) // Deep Violet

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    
    val phase1State = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2State = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3State = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(34000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p1 = phase1State.value
            val p2 = phase2State.value
            val p3 = phase3State.value

            val width = size.width
            val height = size.height
            val minDim = minOf(width, height)
            val radius = minDim * 0.75f

            // Blob 1: Top Left figure-8
            val cx1 = width * 0.32f + (width * 0.22f) * kotlin.math.sin(p1)
            val cy1 = height * 0.28f + (height * 0.22f) * kotlin.math.cos(p1 * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = Offset(cx1, cy1),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx1, cy1)
            )

            // Blob 2: Center Right orbital
            val cx2 = width * 0.72f + (width * 0.25f) * kotlin.math.cos(p2)
            val cy2 = height * 0.48f + (height * 0.25f) * kotlin.math.sin(p2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = Offset(cx2, cy2),
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f,
                center = Offset(cx2, cy2)
            )

            // Blob 3: Bottom Left sweep
            val cx3 = width * 0.22f + (width * 0.28f) * kotlin.math.sin(p3)
            val cy3 = height * 0.82f + (height * 0.20f) * kotlin.math.cos(p3)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = Offset(cx3, cy3),
                    radius = radius * 1.15f
                ),
                radius = radius * 1.15f,
                center = Offset(cx3, cy3)
            )

            // Blob 4: Bottom Right ambient violet balance
            val cx4 = width * 0.80f + (width * 0.18f) * kotlin.math.sin(p2 * 0.7f)
            val cy4 = height * 0.85f + (height * 0.18f) * kotlin.math.cos(p1 * 0.7f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color4, Color.Transparent),
                    center = Offset(cx4, cy4),
                    radius = radius * 0.85f
                ),
                radius = radius * 0.85f,
                center = Offset(cx4, cy4)
            )
        }

        // Translucent frosted diffusion overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF12141F).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.35f))
        )

        content()
    }
}
