package com.example.ui.components.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val alphaAnim by animateFloatAsState(
        targetValue = if (!enabled) 0.3f else if (isPressed) 0.8f else 0.5f,
        animationSpec = tween(150),
        label = "buttonAlpha"
    )

    val isDark = AppTheme.colors.isDark
    
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6C63FF).copy(alpha = alphaAnim),
            Color(0xFF00E5FF).copy(alpha = alphaAnim)
        )
    )

    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgGradient)
            .border(1.dp, borderGradient, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
