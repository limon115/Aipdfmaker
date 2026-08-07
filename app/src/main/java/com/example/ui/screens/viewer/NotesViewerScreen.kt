package com.example.ui.screens.viewer

import android.content.Intent
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showPreviewModal by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadData(projectId)
    }

    if (showPreviewModal) {
        ExportPreviewModal(
            htmlContent = state.htmlContent,
            onConfirm = {
                showPreviewModal = false
                // 🔥 NUKE THE PERMISSION HIJACK: Go directly to FileProvider export!
                viewModel.exportDocument { pdfFile, htmlFile ->
                    val isPdf = state.outputFormat.equals("pdf", ignoreCase = true)
                    val selectedFile = if (isPdf && pdfFile != null) pdfFile else htmlFile
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        selectedFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        val mimeType = if (isPdf && pdfFile != null) "application/pdf" else "text/html"
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open Document..."))
                }
            },
            onDismiss = { showPreviewModal = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Final Document", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadDataWithBaseURL(null, state.htmlContent, "text/HTML", "UTF-8", null)
                        }
                    },
                    update = { view ->
                        view.loadDataWithBaseURL(null, state.htmlContent, "text/HTML", "UTF-8", null)
                    }
                )
            }
        }
    }
}

@Composable
fun ExportPreviewModal(
    htmlContent: String,
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
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
                            }
                        },
                        update = { view ->
                            view.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
                        }
                    )
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
