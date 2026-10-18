package com.example.ui.screens.math

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassTextField
import com.example.domain.services.pdf.PdfNotificationManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Cancel
import com.example.ui.components.glass.GlassButton
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathSolverScreen(
    onNavigateBack: () -> Unit,
    viewModel: MathSolverViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var problemText by remember { mutableStateOf("") }
    var pdfTitle by remember { mutableStateOf("") }
    var showTitleDialog by remember { mutableStateOf(false) }
    var isBackgroundAction by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pdfNotificationManager = remember { PdfNotificationManager(context) }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(state) {
        when (val currentState = state) {
            is MathSolverState.Success -> {
                pdfNotificationManager.showSuccessNotification("Math Solution", currentState.pdfFile)
            }
            is MathSolverState.Error -> {
                pdfNotificationManager.showErrorNotification("Math Solution", currentState.message)
            }
            else -> {}
        }
    }

    if (showTitleDialog) {
        var dialogTitle by remember(showTitleDialog) {
            mutableStateOf(pdfTitle.ifBlank { suggestMathTitle(problemText) })
        }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Title for Solved Math PDF",
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Specify a title for the solved math document. This will be used in the PDF header and saved file name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("PDF Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Saved under: /storage/emulated/0/Documents/aipdfs/mathsolver/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTitleDialog = false
                        val finalTitle = dialogTitle.trim().ifEmpty { suggestMathTitle(problemText) }
                        pdfTitle = finalTitle
                        if (isBackgroundAction) {
                            viewModel.solveProblemInBackground(context, problemText, finalTitle)
                        } else {
                            viewModel.solveProblem(context, problemText, finalTitle)
                        }
                    }
                ) {
                    Text(if (isBackgroundAction) "Solve in Background" else "Solve & Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Math Solver", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AI Math Tutor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a math problem. The AI will explain the theory, solve it step-by-step, and generate a PDF solution sheet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            val sampleProblems = remember {
                listOf(
                    "Solve: d²y/dx² + 4y = 0",
                    "Calculate integral: ∫ (x³ + 2x) dx",
                    "Find derivative: f(x) = sin(x²)",
                    "Eigenvalues of matrix [[1, 2], [2, 1]]"
                )
            }

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                items(sampleProblems, key = { it }) { sample ->
                    AssistChip(
                        onClick = { problemText = sample },
                        label = { Text(sample, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            GlassTextField(
                value = pdfTitle,
                onValueChange = { pdfTitle = it },
                label = { Text("PDF Document Title (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            GlassTextField(
                value = problemText,
                onValueChange = { problemText = it },
                label = { Text("Enter Math Problem") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (problemText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { 
                        problemText = ""
                        pdfTitle = ""
                    }) {
                        Text("Clear Input", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "mathSolverStateTransition"
            ) { currentState ->
                when (currentState) {
                    is MathSolverState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isBackgroundAction = false
                                    showTitleDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                enabled = problemText.isNotBlank(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Solve & Generate PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            GlassButton(
                                onClick = {
                                    isBackgroundAction = true
                                    showTitleDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                enabled = problemText.isNotBlank(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Run in Background",
                                        tint = androidx.compose.ui.graphics.Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Solve in Background",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    is MathSolverState.Processing -> {
                        MathSolverProgressCard(
                            progress = currentState.progress,
                            statusText = currentState.statusText,
                            title = "Solving Math Problem",
                            onSwitchBackground = { viewModel.solveProblemInBackground(context, problemText, pdfTitle.ifBlank { null }) },
                            onCancel = { viewModel.cancelSolving(context) }
                        )
                    }
                    is MathSolverState.CompilingPdf -> {
                        MathSolverProgressCard(
                            progress = currentState.progress,
                            statusText = currentState.statusText,
                            title = "Compiling PDF Solution",
                            onSwitchBackground = { viewModel.solveProblemInBackground(context, problemText, pdfTitle.ifBlank { null }) },
                            onCancel = { viewModel.cancelSolving(context) }
                        )
                    }
                    is MathSolverState.EnqueuedInBackground -> {
                        MathSolverProgressCard(
                            progress = currentState.progress,
                            statusText = currentState.statusText,
                            title = "Running in Background",
                            onSwitchBackground = null,
                            onCancel = { viewModel.cancelSolving(context) }
                        )
                    }
                    is MathSolverState.Success -> {
                        val pdfFile = currentState.pdfFile
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "PDF Solution Sheet Ready!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    pdfFile.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Size: ${com.example.utils.FileUtils.formatFileSize(pdfFile.length())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Saved under:\n${pdfFile.absolutePath}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        openPdf(context, pdfFile)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open PDF")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { 
                                        problemText = ""
                                        pdfTitle = ""
                                        viewModel.resetState() 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Solve Another Problem")
                                }
                            }
                        }
                    }
                    is MathSolverState.Error -> {
                        val errorMsg = currentState.message
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.resetState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openPdf(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open Math Solution"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun MathSolverProgressCard(
    progress: Float,
    statusText: String,
    title: String,
    onSwitchBackground: (() -> Unit)?,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "progressAnimation"
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.components.WavyLinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onSwitchBackground != null) {
                    GlassButton(
                        onClick = onSwitchBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Run in Background", tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Switch to Background Task",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancel Task")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun suggestMathTitle(problem: String): String {
    val clean = problem.lines()
        .firstOrNull { it.isNotBlank() }
        ?.replace(Regex("[^a-zA-Z0-9 ]"), " ")
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.take(40) ?: "Math Solution"
    return if (clean.startsWith("Math", ignoreCase = true)) clean else "Math: $clean"
}
