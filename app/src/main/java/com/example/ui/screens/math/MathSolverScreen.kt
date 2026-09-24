package com.example.ui.screens.math

import android.content.Intent
import androidx.compose.foundation.layout.*
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

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Cancel
import com.example.ui.components.glass.GlassButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathSolverScreen(
    onNavigateBack: () -> Unit,
    viewModel: MathSolverViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var problemText by remember { mutableStateOf("") }
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

            GlassTextField(
                value = problemText,
                onValueChange = { problemText = it },
                label = { Text("Enter Math Problem") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                is MathSolverState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.solveProblem(context, problemText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = problemText.isNotBlank(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Solve & Generate PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        GlassButton(
                            onClick = { viewModel.solveProblemInBackground(context, problemText) },
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
                    val procState = state as MathSolverState.Processing
                    MathSolverProgressCard(
                        progress = procState.progress,
                        statusText = procState.statusText,
                        title = "Solving Math Problem",
                        onSwitchBackground = { viewModel.solveProblemInBackground(context, problemText) },
                        onCancel = { viewModel.cancelSolving(context) }
                    )
                }
                is MathSolverState.CompilingPdf -> {
                    val compileState = state as MathSolverState.CompilingPdf
                    MathSolverProgressCard(
                        progress = compileState.progress,
                        statusText = compileState.statusText,
                        title = "Compiling PDF Solution",
                        onSwitchBackground = { viewModel.solveProblemInBackground(context, problemText) },
                        onCancel = { viewModel.cancelSolving(context) }
                    )
                }
                is MathSolverState.EnqueuedInBackground -> {
                    val bgState = state as MathSolverState.EnqueuedInBackground
                    MathSolverProgressCard(
                        progress = bgState.progress,
                        statusText = bgState.statusText,
                        title = "Running in Background",
                        onSwitchBackground = null,
                        onCancel = { viewModel.cancelSolving(context) }
                    )
                }
                is MathSolverState.Success -> {
                    val pdfFile = (state as MathSolverState.Success).pdfFile
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Success!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your PDF solution sheet is ready.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    openPdf(context, pdfFile)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open PDF")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { 
                                problemText = ""
                                viewModel.resetState() 
                            }) {
                                Text("Solve Another Problem")
                            }
                        }
                    }
                }
                is MathSolverState.Error -> {
                    val errorMsg = (state as MathSolverState.Error).message
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
