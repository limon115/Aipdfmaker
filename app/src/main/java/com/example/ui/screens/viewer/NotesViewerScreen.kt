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
import com.example.ui.components.UndoRedoManager
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
    var fontSizeMenuExpanded by remember { mutableStateOf(false) }

    // Search inside code editor
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val searchMatches = remember(editorValue.text, searchQuery) {
        if (searchQuery.isBlank()) emptyList<TextRange>()
        else {
            val matches = mutableListOf<TextRange>()
            var idx = 0
            while (idx < editorValue.text.length) {
                val found = editorValue.text.indexOf(searchQuery, idx, ignoreCase = true)
                if (found == -1) break
                val end = (found + searchQuery.length).coerceAtMost(editorValue.text.length)
                matches.add(TextRange(found, end))
                idx = maxOf(idx + 1, end)
            }
            matches
        }
    }

    val currentMatchRange = searchMatches.getOrNull(
        if (searchMatches.isEmpty()) 0 else currentMatchIndex.coerceIn(0, searchMatches.lastIndex)
    )

    // Debounced autosave mechanism
    LaunchedEffect(editorValue.text) {
        if (isInitialContentLoaded && editorValue.text != state.latexContent) {
            saveStatus = EditorSaveStatus.SAVING
            delay(800) // 800ms debounce
            viewModel.saveLatexContent(editorValue.text) {
                saveStatus = EditorSaveStatus.SAVED
            }
        }
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
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Fixed bottom actions: View PDF and Recompile (Primary Action)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Source Tab
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTab == 0) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedTab = 0 }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = "Source",
                                            tint = if (selectedTab == 0) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Source",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedTab == 0) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Preview Tab
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTab == 1) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedTab = 1 }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "Preview",
                                            tint = if (selectedTab == 1) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Preview",
                                            style = MaterialTheme.typography.labelLarge,
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

                        // Tab 0: Editable LaTeX Source Code Editor Card
                        if (selectedTab == 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Card Toolbar Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LaTeX source",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Save status indicator
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                when (saveStatus) {
                                                    EditorSaveStatus.SAVED -> {
                                                        Icon(
                                                            Icons.Filled.CheckCircle,
                                                            contentDescription = "Saved",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = "All changes saved",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF334155),
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    }
                                                    EditorSaveStatus.SAVING -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp,
                                                            color = Color(0xFF2563EB)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = "Saving...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                    EditorSaveStatus.UNSAVED -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(Color(0xFFF59E0B), CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = "Unsaved edits",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF64748B)
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
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${fontSizeScale.toInt()}sp",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Icon(
                                                            Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "Font size",
                                                            tint = Color(0xFF64748B),
                                                            modifier = Modifier.size(16.dp)
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
                                                                fontSizeScale = size
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
                                                modifier = Modifier.clickable {
                                                    isSearchOpen = !isSearchOpen
                                                    if (!isSearchOpen) searchQuery = ""
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Search,
                                                        contentDescription = "Search in code",
                                                        tint = if (isSearchOpen) Color(0xFF2563EB) else Color(0xFF64748B),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Expandable Search Bar
                                    AnimatedVisibility(visible = isSearchOpen) {
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
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
                                                    onValueChange = { searchQuery = it },
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
                                                        onClick = {
                                                            if (searchMatches.isNotEmpty()) {
                                                                currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                                                val match = searchMatches[currentMatchIndex]
                                                                editorValue = editorValue.copy(selection = match)
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (searchMatches.isNotEmpty()) {
                                                                currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                                                val match = searchMatches[currentMatchIndex]
                                                                editorValue = editorValue.copy(selection = match)
                                                            }
                                                        },
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
                                                    onClick = {
                                                        isSearchOpen = false
                                                        searchQuery = ""
                                                    },
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
                                        value = editorValue,
                                        onValueChange = { newValue ->
                                            editorValue = newValue
                                            saveStatus = EditorSaveStatus.UNSAVED
                                            viewModel.updateLatexContent(newValue.text)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        fontSize = fontSizeScale,
                                        searchQuery = searchQuery,
                                        currentMatchRange = currentMatchRange,
                                        undoRedoManager = undoRedoManager,
                                        onUndoStateChange = {
                                            saveStatus = EditorSaveStatus.UNSAVED
                                            viewModel.updateLatexContent(editorValue.text)
                                        }
                                    )
                                }
                            }
                        } else {
                            // Tab 1: Live Document PDF Preview Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                val pdfFile = state.generatedPdfFile
                                if (pdfFile != null && pdfFile.exists()) {
                                    PdfDocumentPreview(
                                        pdfFile = pdfFile,
                                        recompileKey = state.recompileTimestamp,
                                        modifier = Modifier.fillMaxSize(),
                                        onOpenExternal = {
                                            FileUtils.openPdf(context, pdfFile, "Open ${state.project?.title ?: "PDF"}")
                                        }
                                    )
                                } else {
                                    // Empty state when PDF hasn't been generated yet
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color(0xFF2563EB).copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No Document Preview Yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Switch to the Source tab to edit LaTeX code, then tap Recompile below to build and view the document.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = handleRecompile,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Compile Document", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
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
