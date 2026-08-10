import os

content = """package com.example.ui.screens.processing

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
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
        containerColor = Color.Black // True-black background
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

            // The Glowing Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                val ringColor = when (state.workState) {
                    WorkInfo.State.FAILED -> Color(0xFFEF4444) // Red
                    WorkInfo.State.SUCCEEDED -> Color(0xFF10B981) // Green
                    else -> Color(0xFF3B82F6) // Blue
                }

                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.05f),
                    strokeWidth = 8.dp
                )

                CircularProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxSize(),
                    color = ringColor,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "${(progressAnim.value * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Cycling Status Text with Animation
            AnimatedContent(
                targetState = statusIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "status_text"
            ) { targetIndex ->
                val textToShow = when (state.workState) {
                    WorkInfo.State.FAILED -> "Processing failed."
                    WorkInfo.State.SUCCEEDED -> "Blueprint complete."
                    else -> statusTexts[targetIndex]
                }
                Text(
                    text = textToShow,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Background Button (Glassmorphism)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .clickable { onNavigateBack() }
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Run in Background",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
"""
with open("app/src/main/java/com/example/ui/screens/processing/ProcessingScreen.kt", "w") as f:
    f.write(content)
