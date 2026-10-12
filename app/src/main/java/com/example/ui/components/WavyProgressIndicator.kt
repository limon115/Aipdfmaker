package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.glass.GlassCard
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

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
 * Indeterminate wavy linear progress indicator.
 * Smoothly animates a sine wave across the track continuously.
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

/**
 * Modern wavy circular progress indicator with determinate progress support.
 * Renders an animated wavy track and active progress arc that flows dynamically around the circle.
 */
@Composable
fun WavyCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    strokeWidth: Dp = 8.dp,
    waveAmplitude: Dp = 6.dp,
    waveCount: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyCircularProgressTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyCircularProgressPhase"
    )

    Canvas(modifier = modifier) {
        val currentProgress = progress().coerceIn(0f, 1f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidthPx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val baseRadius = (minOf(size.width, size.height) / 2f) - ampPx - strokeWidthPx / 2f
        if (baseRadius <= 0f) return@Canvas

        // Draw track circle (subtle background ring)
        drawCircle(
            color = trackColor,
            radius = baseRadius,
            center = center,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )

        // Draw active animated wavy progress arc
        if (currentProgress > 0.005f) {
            val wavePath = Path()
            val startAngle = -PI / 2.0 // Start at top (12 o'clock)
            val sweepAngle = 2 * PI * currentProgress
            val totalSteps = (180 * currentProgress).toInt().coerceAtLeast(12)

            for (i in 0..totalSteps) {
                val fraction = i.toFloat() / totalSteps
                val angle = startAngle + sweepAngle * fraction
                val angleFromStart = sweepAngle * fraction

                // Radius modulated by sine wave along the circular arc
                val r = baseRadius + ampPx * sin(waveCount * angleFromStart - phase).toFloat()
                val x = (center.x + r * cos(angle)).toFloat()
                val y = (center.y + r * sin(angle)).toFloat()

                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }

            // If full circle (100%), close path smoothly
            if (currentProgress >= 0.999f) {
                wavePath.close()
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
 * Overload accepting direct Float progress.
 */
@Composable
fun WavyCircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    strokeWidth: Dp = 8.dp,
    waveAmplitude: Dp = 6.dp,
    waveCount: Int = 12
) {
    WavyCircularProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        waveAmplitude = waveAmplitude,
        waveCount = waveCount
    )
}

/**
 * Modern wavy circular progress indicator for icons, badges, or small circular loaders.
 */
@Composable
fun WavyCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    strokeWidth: Dp = 3.dp,
    waveAmplitude: Dp = 1.5.dp,
    waveCount: Int = 6
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyCircularTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyCircularPhase"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyCircularRotation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidthPx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val baseRadius = (minOf(size.width, size.height) / 2f) - ampPx - strokeWidthPx
        if (baseRadius <= 0f) return@Canvas

        // Subtle track circle
        drawCircle(
            color = trackColor,
            radius = baseRadius,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )

        // Animated wavy ring
        val path = Path()
        val steps = 60
        val rotRad = Math.toRadians(rotation.toDouble()).toFloat()

        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2 * PI).toFloat()
            val r = baseRadius + ampPx * sin(waveCount * angle - phase)
            val currentAngle = angle + rotRad
            val x = center.x + r * kotlin.math.cos(currentAngle)
            val y = center.y + r * kotlin.math.sin(currentAngle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Full-screen glassmorphic loading screen featuring a wavy progress bar.
 */
@Composable
fun WavyLoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier,
    subMessage: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    WavyLinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp),
                        color = color,
                        strokeWidth = 4.dp,
                        waveAmplitude = 4.dp,
                        wavePeriod = 24.dp
                    )

                    if (subMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = subMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standalone glassmorphic card with a wavy progress bar.
 */
@Composable
fun WavyLoadingCard(
    message: String,
    modifier: Modifier = Modifier,
    subMessage: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            WavyLinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                color = color,
                strokeWidth = 4.dp,
                waveAmplitude = 3.5.dp
            )
            if (subMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Compact inline wavy progress bar with an optional message.
 */
@Composable
fun WavyLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 3.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        WavyLinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(waveAmplitude * 2 + strokeWidth + 4.dp),
            color = color,
            strokeWidth = strokeWidth,
            waveAmplitude = waveAmplitude
        )
    }
}
