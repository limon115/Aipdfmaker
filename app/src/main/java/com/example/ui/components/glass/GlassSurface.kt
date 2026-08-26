package com.example.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    
    // Linear gradient background
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color(0xFF161922).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
            if (isDark) Color(0xFF161922).copy(alpha = alpha * 0.7f) else Color.White.copy(alpha = alpha * 0.8f)
        )
    )

    // Gradient border stroke
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.2f else 0.5f),
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = if (isDark) 0.1f else 0.3f)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(bgGradient)
            .border(1.dp, borderGradient, shape)
    ) {
        // Subtle internal blur to frost the background behind the surface
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Color.Transparent)
        )
        content()
    }
}
