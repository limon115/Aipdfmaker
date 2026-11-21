package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    if (!showShimmer) return Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 1000f, translateAnim.value - 1000f),
        end = Offset(translateAnim.value, translateAnim.value)
    )
}

fun Modifier.shimmer(): Modifier = composed {
    background(shimmerBrush())
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .shimmer()
    )
}

@Composable
fun SkeletonProjectCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(modifier = Modifier.width(120.dp), height = 24.dp)
            SkeletonBox(modifier = Modifier.size(24.dp), shape = CircleShape)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f), height = 16.dp)
    }
}

@Composable
fun SkeletonChatMessage() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f), height = 16.dp)
        }
    }
}
