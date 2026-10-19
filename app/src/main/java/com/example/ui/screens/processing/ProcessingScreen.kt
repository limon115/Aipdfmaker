package com.example.ui.screens.processing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import com.example.ui.components.WavyCircularProgressIndicator
import com.example.ui.components.WavyLinearProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    viewModel: ProcessingViewModel = viewModel(),
    projectId: Int,
    onProcessingFinished: (com.example.domain.models.BlueprintSummary?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startProcessing(context, projectId)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            onProcessingFinished(state.blueprintSummary)
        }
    }

    // Fake Progress Animation
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(state.workState) {
        when (state.workState) {
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, null -> {
                if (progressAnim.value < 0.95f) {
                    launch { progressAnim.animateTo(0.40f, animationSpec = tween(800, easing = FastOutSlowInEasing)) }
                    delay(800)
                    launch { progressAnim.animateTo(0.85f, animationSpec = tween(3000, easing = LinearOutSlowInEasing)) }
                    delay(3000)
                    launch { progressAnim.animateTo(0.95f, animationSpec = tween(8000, easing = LinearOutSlowInEasing)) }
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                progressAnim.animateTo(1f, animationSpec = tween(400))
            }
            WorkInfo.State.FAILED -> {
                // Keep progress where it is but change color
            }
            else -> {}
        }
    }

    // Cycling Status Text
    val statusTexts = listOf(
        "Extracting raw text...",
        "Analyzing syllabus structure...",
        "Synthesizing Blueprint...",
        "Mapping knowledge graphs..."
    )
    var statusIndex by remember { mutableStateOf(0) }

    LaunchedEffect(state.workState) {
        while (state.workState != WorkInfo.State.SUCCEEDED && state.workState != WorkInfo.State.FAILED) {
            delay(2500)
            statusIndex = (statusIndex + 1) % statusTexts.size
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val ringColor = when (state.workState) {
                WorkInfo.State.FAILED -> Color(0xFFEF4444) // Red
                WorkInfo.State.SUCCEEDED -> Color(0xFF10B981) // Green
                else -> Color(0xFF3B82F6) // Blue
            }

            // The Glowing Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                WavyCircularProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxSize(),
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                    strokeWidth = 8.dp,
                    waveAmplitude = 6.dp,
                    waveCount = 12
                )

                Text(
                    text = "${(progressAnim.value * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            WavyLinearProgressIndicator(
                progress = { progressAnim.value },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp),
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                strokeWidth = 6.dp,
                waveAmplitude = 4.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Cycling Status Text with Animation
            AnimatedContent(
                targetState = if (state.workState == WorkInfo.State.FAILED) "failed" else statusIndex.toString(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "status_text"
            ) { _ ->
                val textToShow = when (state.workState) {
                    WorkInfo.State.FAILED -> state.errorMessage ?: "Processing failed."
                    WorkInfo.State.SUCCEEDED -> "Blueprint complete."
                    else -> statusTexts[statusIndex]
                }
                Text(
                    text = textToShow,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.workState == WorkInfo.State.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (state.workState == WorkInfo.State.FAILED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.retry(context, projectId) },
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Go Back",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Run in Background",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.cancelProcessing(context, projectId)
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChecklistRow(item: ChecklistItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            when (item.state) {
                StepState.COMPLETED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)), // Success Green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                StepState.IN_PROGRESS -> {
                    com.example.ui.components.WavyCircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        waveAmplitude = 1.5.dp
                    )
                }
                StepState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
                StepState.FAILED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.state == StepState.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else if (item.state == StepState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (item.state == StepState.IN_PROGRESS || item.state == StepState.FAILED) FontWeight.Bold else FontWeight.Normal
        )
    }
}
