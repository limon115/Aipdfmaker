package com.example.ui.components.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    
    // Modern ambient backdrop glow
    val bgGlow = Brush.radialGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f),
            if (isDark) Color(0xFF161924).copy(alpha = 0.75f) else Color(0xFFF8FAFC).copy(alpha = 0.70f)
        ),
        radius = 900f
    )

    // Modern 3-stop specular glass refraction border
    val shimmerBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.45f else 0.80f),
            Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
            Color.White.copy(alpha = if (isDark) 0.25f else 0.50f)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Box(
        modifier = modifier
            .clip(shape)
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
    shape: Shape = RoundedCornerShape(24.dp),
    colors: Any? = null,
    elevation: Any? = null,
    border: Any? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppTheme.colors.isDark
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassCardScale"
    )

    val bgGlow = Brush.radialGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = if (isPressed) 0.12f else 0.08f) else Color.White.copy(alpha = if (isPressed) 0.85f else 0.65f),
            if (isDark) Color(0xFF161924).copy(alpha = 0.75f) else Color(0xFFF8FAFC).copy(alpha = 0.70f)
        ),
        radius = 900f
    )

    val shimmerBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) (if (isPressed) 0.65f else 0.45f) else (if (isPressed) 0.95f else 0.80f)),
            Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
            Color.White.copy(alpha = if (isDark) 0.25f else 0.50f)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(shape)
            .background(bgGlow)
            .border(1.dp, shimmerBorder, shape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
    ) {
        Column(content = content)
    }
}

