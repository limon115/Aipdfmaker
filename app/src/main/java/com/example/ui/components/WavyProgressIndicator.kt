package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Modern expressive wavy linear progress indicator.
 * Renders an animated sine wave track that flows dynamically.
 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 3.dp,
    wavePeriod: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyProgressTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveAmplitude * 2 + strokeWidth + 4.dp)
    ) {
        val currentProgress = progress().coerceIn(0f, 1f)
        val width = size.width
        val centerY = size.height / 2f
        val strokeWidthPx = strokeWidth.toPx()
        val amplitudePx = waveAmplitude.toPx()
        val periodPx = wavePeriod.toPx().coerceAtLeast(10f)

        // Draw background track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )

        // Draw animated wavy progress active line
        if (currentProgress > 0.005f) {
            val progressWidth = width * currentProgress
            val wavePath = Path()
            val stepPx = 3f

            var x = 0f
            val startY = centerY + amplitudePx * sin(2 * PI * (0f / periodPx) - phase).toFloat()
            wavePath.moveTo(0f, startY)

            while (x <= progressWidth) {
                val y = centerY + amplitudePx * sin(2 * PI * (x / periodPx) - phase).toFloat()
                wavePath.lineTo(x, y)
                x += stepPx
            }

            if (x < progressWidth + stepPx) {
                val endY = centerY + amplitudePx * sin(2 * PI * (progressWidth / periodPx) - phase).toFloat()
                wavePath.lineTo(progressWidth, endY)
            }

            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Overload accepting a direct Float progress value.
 */
@Composable
fun WavyLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 3.dp,
    wavePeriod: Dp = 20.dp
) {
    WavyLinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        waveAmplitude = waveAmplitude,
        wavePeriod = wavePeriod
    )
}

/**
 * Indeterminate wavy progress indicator.
 */
@Composable
fun WavyLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 3.dp,
    wavePeriod: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "IndeterminateWavyTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IndeterminateWavyPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveAmplitude * 2 + strokeWidth + 4.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val strokeWidthPx = strokeWidth.toPx()
        val amplitudePx = waveAmplitude.toPx()
        val periodPx = wavePeriod.toPx().coerceAtLeast(10f)

        // Subtle track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )

        // Continuous wavy line across the whole bar
        val wavePath = Path()
        val stepPx = 3f
        var x = 0f
        val startY = centerY + amplitudePx * sin(2 * PI * (0f / periodPx) - phase).toFloat()
        wavePath.moveTo(0f, startY)

        while (x <= width) {
            val y = centerY + amplitudePx * sin(2 * PI * (x / periodPx) - phase).toFloat()
            wavePath.lineTo(x, y)
            x += stepPx
        }

        drawPath(
            path = wavePath,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
