package com.example.ui.components.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    // Base background colors
    val bgColor = if (isDark) Color(0xFF0A0C14) else Color(0xFFF0F2F5)
    
    // Blob colors based on SKILL.md specs
    val color1 = Color(0xFF6C63FF).copy(alpha = if (isDark) 0.4f else 0.6f) // Purple
    val color2 = Color(0xFF00E5FF).copy(alpha = if (isDark) 0.3f else 0.5f) // Cyan
    val color3 = Color(0xFFFF5252).copy(alpha = if (isDark) 0.25f else 0.4f) // Red

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Draw the liquid blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val minDim = minOf(width, height)
            val radius = minDim * 0.7f

            // Blob 1: Top Left moving in a figure-8
            val cx1 = width * 0.3f + (width * 0.2f) * kotlin.math.sin(phase1)
            val cy1 = height * 0.3f + (height * 0.2f) * kotlin.math.cos(phase1 * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = Offset(cx1, cy1),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx1, cy1)
            )

            // Blob 2: Center Right moving circularly
            val cx2 = width * 0.7f + (width * 0.25f) * kotlin.math.cos(phase2)
            val cy2 = height * 0.5f + (height * 0.25f) * kotlin.math.sin(phase2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = Offset(cx2, cy2),
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = Offset(cx2, cy2)
            )

            // Blob 3: Bottom Left moving
            val cx3 = width * 0.2f + (width * 0.3f) * kotlin.math.sin(phase3)
            val cy3 = height * 0.8f + (height * 0.2f) * kotlin.math.cos(phase3)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = Offset(cx3, cy3),
                    radius = radius * 1.1f
                ),
                radius = radius * 1.1f,
                center = Offset(cx3, cy3)
            )
        }

        // Noise/Overlay to add texture and ensure readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF161922).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f))
        )

        content()
    }
}
