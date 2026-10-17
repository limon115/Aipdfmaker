package com.example.ui.screens.viewer

import com.example.domain.services.pdf.PdfNotificationManager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.utils.FileUtils
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesViewerScreen(
    viewModel: NotesViewerViewModel,
    projectId: Int,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val context = LocalContext.current
    var showPreviewModal by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadData(projectId)
    }

        val pdfNotificationManager = remember { PdfNotificationManager(context) }

    val performExport = {
        showPreviewModal = false
        viewModel.exportDocument(
            onComplete = { pdfFile, texFile ->
                try {
                    val isPdf = state.outputFormat.equals("pdf", ignoreCase = true)
                    if (isPdf && pdfFile == null) {
                        android.widget.Toast.makeText(context, "PDF generation failed.", android.widget.Toast.LENGTH_SHORT).show()
                        pdfNotificationManager.showErrorNotification(state.project?.title ?: "Project", "PDF generation failed")
                        return@exportDocument
                    }
                    val selectedFile = if (isPdf && pdfFile != null) pdfFile else texFile
                    
                    if (isPdf && pdfFile != null) {
                        pdfNotificationManager.showSuccessNotification(state.project?.title ?: "Project", pdfFile)
                    }
                    
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        selectedFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        val mimeType = if (isPdf && pdfFile != null) "application/pdf" else "text/plain"
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open Document..."))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onNavigateHome()
            },
            onError = { errorMsg ->
                android.widget.Toast.makeText(context, "Export Failed: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                pdfNotificationManager.showErrorNotification(state.project?.title ?: "Project", errorMsg)
            }
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Proceed regardless, but notification might not show if denied
        performExport()
    }
    
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                performExport()
            } else {
                performExport() 
            }
        }
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Continue regardless of whether they granted notification permission
    }

    val checkAndPerformExport = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                performExport()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        } else {
            performExport()
        }
    }

    if (showPreviewModal) {
        ExportPreviewModal(
            latexContent = state.latexContent,
            onConfirm = {
                checkAndPerformExport()
            },
            onDismiss = { showPreviewModal = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Final Document", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val pdfFile = state.generatedPdfFile
                    if (pdfFile != null && pdfFile.exists()) {
                        IconButton(onClick = {
                            FileUtils.openPdf(context, pdfFile, "Open ${state.project?.title ?: "PDF"}")
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "View PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (state.latexContent.isNotBlank()) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("LaTeX Code", state.latexContent)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "LaTeX code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy LaTeX Code", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            val pdfFile = state.generatedPdfFile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (pdfFile != null && pdfFile.exists()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                FileUtils.openPdf(context, pdfFile, "Open ${state.project?.title ?: "PDF"}")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View PDF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showPreviewModal = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Recompile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { showPreviewModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Export Final Document", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        var fontSizeScale by remember { mutableFloatStateOf(12f) }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                com.example.ui.components.WavyLoadingScreen(
                    message = "Loading LaTeX Document...",
                    subMessage = "Parsing snippets and compiling preview"
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LaTeX Source Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                if (fontSizeScale > 8f) fontSizeScale -= 2f
                            }) {
                                Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                text = "${fontSizeScale.toInt()}sp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                if (fontSizeScale < 24f) fontSizeScale += 2f
                            }) {
                                Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Text(
                                text = state.latexContent,
                                fontSize = androidx.compose.ui.unit.TextUnit(fontSizeScale, androidx.compose.ui.unit.TextUnitType.Sp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
    if (state.isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(32.dp)
            ) {
                Text("Generating PDF...", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (state.exportProgress > 0f) {
                    com.example.ui.components.WavyLinearProgressIndicator(
                        progress = { state.exportProgress },
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${(state.exportProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    com.example.ui.components.WavyLinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
}

@Composable
fun ExportPreviewModal(
    latexContent: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Export Preview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = latexContent,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Confirm Export")
                    }
                }
            }
        }
    }
}
