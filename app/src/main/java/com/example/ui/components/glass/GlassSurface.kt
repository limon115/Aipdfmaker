package com.example.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color(0xFF161924).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
            if (isDark) Color(0xFF10121A).copy(alpha = alpha * 0.75f) else Color.White.copy(alpha = alpha * 0.85f)
        )
    )

    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.70f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = if (isDark) 0.20f else 0.40f)
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 800f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgGradient)
            .border(1.dp, borderGradient, shape)
    ) {
        content()
    }
}
