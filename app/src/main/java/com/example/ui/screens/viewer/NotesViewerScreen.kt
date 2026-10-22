package com.example.ui.screens.viewer

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.services.pdf.PdfNotificationManager
import com.example.ui.components.LatexCodeEditor
import com.example.ui.components.LatexSearchIndex
import com.example.ui.components.UndoRedoManager
import com.example.ui.components.glass.GlassBackground
import com.example.utils.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class EditorSaveStatus {
    SAVED,
    SAVING,
    UNSAVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesViewerScreen(
    viewModel: NotesViewerViewModel,
    projectId: Int,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(projectId) {
        viewModel.loadData(projectId)
    }

    val pdfNotificationManager = remember { PdfNotificationManager(context) }

    // Tab state: 0 = Source, 1 = Preview
    var selectedTab by remember { mutableIntStateOf(0) }

    // Real Code Editor state
    var editorValue by remember { mutableStateOf(TextFieldValue(state.latexContent)) }
    var isInitialContentLoaded by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf(EditorSaveStatus.SAVED) }
    val undoRedoManager = remember { UndoRedoManager() }

    // Sync state into editorValue on initial load
    LaunchedEffect(state.isLoading, state.latexContent) {
        if (!state.isLoading && !isInitialContentLoaded && state.latexContent.isNotEmpty()) {
            val initial = TextFieldValue(
                text = state.latexContent,
                selection = TextRange(0)
            )
            editorValue = initial
            undoRedoManager.reset(initial)
            isInitialContentLoaded = true
            saveStatus = EditorSaveStatus.SAVED
        }
    }

    // Font size scaling control (10sp - 20sp, default 12sp)
    var fontSizeScale by remember { mutableFloatStateOf(12f) }

    // In-memory cached search index upon viewing and editing LaTeX code
    val searchIndex = remember(editorValue.text) {
        LatexSearchIndex(editorValue.text)
    }

    // Search inside code editor
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val searchMatches = remember(searchIndex, searchQuery) {
        searchIndex.search(searchQuery)
    }

    val currentMatchRange = searchMatches.getOrNull(
        if (searchMatches.isEmpty()) 0 else currentMatchIndex.coerceIn(0, searchMatches.lastIndex)
    )

    // Fast explicit save function: persists directly to Room DB on user demand without continuous keystroke lag
    val performSave: () -> Unit = {
        if (saveStatus != EditorSaveStatus.SAVING) {
            saveStatus = EditorSaveStatus.SAVING
            viewModel.saveLatexContent(editorValue.text) {
                saveStatus = EditorSaveStatus.SAVED
                Toast.makeText(context, "LaTeX document saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val handleUndo: () -> Unit = {
        val undone = undoRedoManager.undo(editorValue)
        if (undone != null) {
            editorValue = undone
            saveStatus = EditorSaveStatus.UNSAVED
        }
    }

    val handleRedo: () -> Unit = {
        val redone = undoRedoManager.redo(editorValue)
        if (redone != null) {
            editorValue = redone
            saveStatus = EditorSaveStatus.UNSAVED
        }
    }

    BackHandler {
        if (saveStatus == EditorSaveStatus.UNSAVED) {
            viewModel.saveLatexContent(editorValue.text)
        }
        onNavigateBack()
    }

    // Export execution
    val performExport = {
        viewModel.exportDocument(
            onComplete = { pdfFile, _ ->
                try {
                    val isPdf = state.outputFormat.equals("pdf", ignoreCase = true)
                    if (isPdf && pdfFile == null) {
                        Toast.makeText(context, "PDF compilation failed.", Toast.LENGTH_SHORT).show()
                        pdfNotificationManager.showErrorNotification(state.project?.title ?: "Project", "PDF compilation failed")
                        return@exportDocument
                    }

                    if (isPdf && pdfFile != null) {
                        pdfNotificationManager.showSuccessNotification(state.project?.title ?: "Project", pdfFile)
                        Toast.makeText(context, "Document compiled successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onError = { errorMsg ->
                Toast.makeText(context, "Compilation Failed: $errorMsg", Toast.LENGTH_LONG).show()
                pdfNotificationManager.showErrorNotification(state.project?.title ?: "Project", errorMsg)
            }
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        performExport()
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

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

    // Recompile action: strictly guarantees edits are saved before triggering compilation
    val handleRecompile = {
        if (saveStatus != EditorSaveStatus.SAVED || state.hasUnsavedChanges) {
            saveStatus = EditorSaveStatus.SAVING
            viewModel.saveLatexContent(editorValue.text) {
                saveStatus = EditorSaveStatus.SAVED
                checkAndPerformExport()
            }
        } else {
            checkAndPerformExport()
        }
    }

    GlassBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Final Document", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (saveStatus == EditorSaveStatus.UNSAVED) {
                                viewModel.saveLatexContent(editorValue.text)
                            }
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Quick Save action in top bar
                        IconButton(
                            onClick = { performSave() },
                            enabled = saveStatus != EditorSaveStatus.SAVING
                        ) {
                            if (saveStatus == EditorSaveStatus.SAVING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2563EB)
                                )
                            } else {
                                Icon(
                                    if (saveStatus == EditorSaveStatus.SAVED) Icons.Default.Check else Icons.Default.Save,
                                    contentDescription = "Save LaTeX Document",
                                    tint = if (saveStatus == EditorSaveStatus.UNSAVED) Color(0xFF2563EB) else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        val pdfFile = state.generatedPdfFile
                        IconButton(onClick = {
                            if (pdfFile != null && pdfFile.exists()) {
                                FileUtils.openPdf(context, pdfFile, "Open ${state.project?.title ?: "PDF"}")
                            } else {
                                Toast.makeText(context, "No PDF compiled yet. Tap Recompile to generate one.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "View PDF",
                                tint = Color(0xFF2563EB)
                            )
                        }
                        if (editorValue.text.isNotBlank()) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("LaTeX Code", editorValue.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "LaTeX code copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy LaTeX Code",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                // Fixed bottom actions: View PDF and Recompile (Primary Action)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // View PDF button
                        OutlinedButton(
                            onClick = {
                                val pdfFile = state.generatedPdfFile
                                if (pdfFile != null && pdfFile.exists()) {
                                    FileUtils.openPdf(context, pdfFile, "Open ${state.project?.title ?: "PDF"}")
                                } else {
                                    Toast.makeText(context, "No compiled PDF yet. Tap Recompile first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF0F6FF),
                                contentColor = Color(0xFF2563EB)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Recompile button (Emphasized as Primary Action)
                        Button(
                            onClick = handleRecompile,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recompile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                    com.example.ui.components.WavyLoadingScreen(
                        message = "Loading LaTeX Document...",
                        subMessage = "Parsing snippets and compiling preview"
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp)
                    ) {
                        // Header row: Source / Preview Tab Switcher & Editable Status Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Segmented Source / Preview Tabs
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Source Tab
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTab == 0) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        selectedTab = 0
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = "Source",
                                            tint = if (selectedTab == 0) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Source",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedTab == 0) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Preview Tab
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTab == 1) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        if (saveStatus == EditorSaveStatus.UNSAVED) {
                                            saveStatus = EditorSaveStatus.SAVING
                                            viewModel.saveLatexContent(editorValue.text) {
                                                saveStatus = EditorSaveStatus.SAVED
                                            }
                                        }
                                        selectedTab = 1
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "Preview",
                                            tint = if (selectedTab == 1) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Preview",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedTab == 1) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // Editable Status Indicator (prominently indicates code is editable)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Editable",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                // Tab 0: Editable LaTeX Source Code Editor Card
                                LatexEditorCard(
                                    value = editorValue,
                                    onValueChange = { newValue ->
                                        editorValue = newValue
                                        saveStatus = EditorSaveStatus.UNSAVED
                                    },
                                    saveStatus = saveStatus,
                                    onSave = performSave,
                                    onUndo = handleUndo,
                                    onRedo = handleRedo,
                                    fontSizeScale = fontSizeScale,
                                    onFontSizeChange = { fontSizeScale = it },
                                    isSearchOpen = isSearchOpen,
                                    onToggleSearch = {
                                        isSearchOpen = !isSearchOpen
                                        if (!isSearchOpen) searchQuery = ""
                                    },
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = {
                                        searchQuery = it
                                        currentMatchIndex = 0
                                    },
                                    searchMatches = searchMatches,
                                    currentMatchIndex = currentMatchIndex,
                                    currentMatchRange = currentMatchRange,
                                    onNavigateMatch = { direction ->
                                        if (searchMatches.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex + direction + searchMatches.size) % searchMatches.size
                                            val match = searchMatches[currentMatchIndex]
                                            editorValue = editorValue.copy(selection = match)
                                        }
                                    },
                                    onCloseSearch = {
                                        isSearchOpen = false
                                        searchQuery = ""
                                    },
                                    undoRedoManager = undoRedoManager,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    isCompactHeader = false
                                )
                            }
                            1 -> {
                                // Tab 1: Full Live Document PDF Preview Card
                                PdfPreviewCard(
                                    pdfFile = state.generatedPdfFile,
                                    recompileKey = state.recompileTimestamp,
                                    onOpenExternal = {
                                        state.generatedPdfFile?.let { file ->
                                            FileUtils.openPdf(context, file, "Open ${state.project?.title ?: "PDF"}")
                                        }
                                    },
                                    onRecompile = handleRecompile,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    isCompactHeader = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compilation progress overlay
        if (state.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(32.dp)
                ) {
                    Text(
                        "Compiling Document...",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
                    Text(
                        "Applying XeLaTeX typesetting engine...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Embedded PDF Document Preview renderer using Android's native [PdfRenderer].
 * Renders pages into high-resolution bitmaps and presents them inside styled cards.
 */
@Composable
fun PdfDocumentPreview(
    pdfFile: File,
    modifier: Modifier = Modifier,
    recompileKey: Long = 0L,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    var pages by remember(pdfFile.absolutePath, recompileKey, pdfFile.lastModified()) {
        mutableStateOf<List<Bitmap>>(emptyList())
    }
    var isLoading by remember(pdfFile.absolutePath, recompileKey, pdfFile.lastModified()) {
        mutableStateOf(true)
    }
    var renderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfFile.absolutePath, recompileKey, pdfFile.lastModified()) {
        isLoading = true
        renderError = null
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    try {
                        val bitmaps = mutableListOf<Bitmap>()
                        val pageCount = renderer.pageCount
                        val density = context.resources.displayMetrics.density
                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            try {
                                val width = (page.width * density * 1.5f).toInt().coerceAtLeast(1)
                                val height = (page.height * density * 1.5f).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmaps.add(bitmap)
                            } finally {
                                page.close()
                            }
                        }
                        withContext(Dispatchers.Main) {
                            pages = bitmaps
                            isLoading = false
                        }
                    } finally {
                        renderer.close()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    renderError = e.message ?: "Failed to render PDF preview"
                    isLoading = false
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Rendering document pages...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    } else if (renderError != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Preview Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = renderError ?: "Unable to render PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenExternal) {
                    Text("Open with External Viewer")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pages.size) { index ->
                val bitmap = pages[index]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${index + 1} of ${pages.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Document Page ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modular card container for the LaTeX Code Editor.
 * Includes header toolbar with autosave status, font-size picker,
 * in-memory cached search bar with match navigation, and code editor.
 */
@Composable
private fun LatexEditorCard(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    saveStatus: EditorSaveStatus,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    fontSizeScale: Float,
    onFontSizeChange: (Float) -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchMatches: List<TextRange>,
    currentMatchIndex: Int,
    currentMatchRange: TextRange?,
    onNavigateMatch: (Int) -> Unit,
    onCloseSearch: () -> Unit,
    undoRedoManager: UndoRedoManager,
    modifier: Modifier = Modifier,
    isCompactHeader: Boolean = false
) {
    var fontSizeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Toolbar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactHeader) 10.dp else 16.dp,
                        vertical = if (isCompactHeader) 8.dp else 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LaTeX source",
                        style = if (isCompactHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // State-based Undo Button
                    IconButton(
                        onClick = onUndo,
                        enabled = undoRedoManager.canUndo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoRedoManager.canUndo) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // State-based Redo Button
                    IconButton(
                        onClick = onRedo,
                        enabled = undoRedoManager.canRedo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (undoRedoManager.canRedo) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Dedicated Fast Save Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (saveStatus) {
                            EditorSaveStatus.UNSAVED -> Color(0xFF2563EB)
                            EditorSaveStatus.SAVING -> Color(0xFFEFF6FF)
                            EditorSaveStatus.SAVED -> Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (saveStatus) {
                                EditorSaveStatus.UNSAVED -> Color(0xFF2563EB)
                                EditorSaveStatus.SAVING -> Color(0xFFBFDBFE)
                                EditorSaveStatus.SAVED -> Color(0xFFE2E8F0)
                            }
                        ),
                        modifier = Modifier
                            .clickable(enabled = saveStatus != EditorSaveStatus.SAVING) {
                                onSave()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (saveStatus == EditorSaveStatus.SAVING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2563EB)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saving...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2563EB)
                                )
                            } else if (saveStatus == EditorSaveStatus.SAVED) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Font-size Dropdown Control
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.clickable { fontSizeMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${fontSizeScale.toInt()}sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Font size",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = fontSizeMenuExpanded,
                            onDismissRequest = { fontSizeMenuExpanded = false }
                        ) {
                            listOf(10f, 12f, 14f, 16f, 18f, 20f).forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("${size.toInt()}sp") },
                                    onClick = {
                                        onFontSizeChange(size)
                                        fontSizeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Search Icon Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = if (isSearchOpen) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        modifier = Modifier.clickable { onToggleSearch() }
                    ) {
                        Box(
                            modifier = Modifier.padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search in code",
                                tint = if (isSearchOpen) Color(0xFF2563EB) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Expandable In-Memory Search Bar
            AnimatedVisibility(visible = isSearchOpen) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Find in LaTeX code...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                inner()
                            }
                        )
                        if (searchMatches.isNotEmpty()) {
                            Text(
                                text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { onNavigateMatch(-1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onNavigateMatch(1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", modifier = Modifier.size(16.dp))
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "No matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onCloseSearch,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Real Multiline Code-Editing Component
            LatexCodeEditor(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                fontSize = fontSizeScale,
                searchQuery = searchQuery,
                currentMatchRange = currentMatchRange,
                undoRedoManager = undoRedoManager,
                onUndoStateChange = null
            )
        }
    }
}

/**
 * Reusable PDF Preview Card used in Full Preview mode.
 * Displays compiled PDF rendered bitmaps or an empty-state card with a Recompile action.
 */
@Composable
private fun PdfPreviewCard(
    pdfFile: File?,
    recompileKey: Long,
    onOpenExternal: () -> Unit,
    onRecompile: () -> Unit,
    modifier: Modifier = Modifier,
    isCompactHeader: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactHeader) 10.dp else 16.dp,
                        vertical = if (isCompactHeader) 8.dp else 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Document preview",
                        style = if (isCompactHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pdfFile != null && pdfFile.exists()) {
                        IconButton(
                            onClick = onOpenExternal,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Open in external viewer",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onRecompile,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recompile", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (pdfFile != null && pdfFile.exists()) {
                PdfDocumentPreview(
                    pdfFile = pdfFile,
                    recompileKey = recompileKey,
                    modifier = Modifier.fillMaxSize(),
                    onOpenExternal = onOpenExternal
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF2563EB).copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Document Preview Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap Recompile to compile the LaTeX code and render the PDF preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRecompile,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compile Document", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
