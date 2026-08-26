import re

content = """package com.example.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CardColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    
    // Radial white glow behind
    val bgGlow = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.05f else 0.4f),
            if (isDark) Color(0xFF161922).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
        ),
        radius = 800f
    )

    // 3-stop shimmer border
    val shimmerBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.4f else 0.7f),
            Color.Transparent,
            Color.White.copy(alpha = if (isDark) 0.2f else 0.4f)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(bgGlow)
            .border(1.dp, shimmerBorder, shape)
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(32.dp),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
    interactionSource: Any? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    
    val bgGlow = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.05f else 0.4f),
            if (isDark) Color(0xFF161922).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
        ),
        radius = 800f
    )

    val shimmerBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.4f else 0.7f),
            Color.Transparent,
            Color.White.copy(alpha = if (isDark) 0.2f else 0.4f)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(bgGlow)
            .border(1.dp, shimmerBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(content = content)
    }
}

"""

with open('app/src/main/java/com/example/ui/components/glass/GlassCard.kt', 'w') as f:
    f.write(content)
