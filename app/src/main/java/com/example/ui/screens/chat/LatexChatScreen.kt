package com.example.ui.screens.chat

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.models.chat.ChatMessage
import com.example.domain.models.chat.ChatPdfStatus
import com.example.domain.models.chat.ChatSession
import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassCard
import com.example.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexChatScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LatexChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    val sessions by viewModel.sessions.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generatingStatus by viewModel.generatingStatus.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val selectedCategoryIndex by viewModel.selectedCategoryIndex.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDeleteId by remember { mutableStateOf<String?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }
    var searchSessionQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Modal Bottom Sheet: Chat History & Sessions
    if (showHistoryDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryDrawer = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Chat History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sessions.size} saved conversation${if (sessions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.startNewChat(context)
                            showHistoryDrawer = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Chat")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search session bar
                OutlinedTextField(
                    value = searchSessionQuery,
                    onValueChange = { searchSessionQuery = it },
                    placeholder = { Text("Search previous chats...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchSessionQuery.isNotEmpty()) {
                            IconButton(onClick = { searchSessionQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredSessions = remember(sessions, searchSessionQuery) {
                    if (searchSessionQuery.isBlank()) sessions
                    else sessions.filter {
                        it.title.contains(searchSessionQuery, ignoreCase = true) ||
                        it.previewText.contains(searchSessionQuery, ignoreCase = true)
                    }
                }

                if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchSessionQuery.isNotBlank()) "No chats found matching '$searchSessionQuery'" else "No conversation history yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            val isSelected = session.id == currentSession?.id
                            SessionItemRow(
                                session = session,
                                isSelected = isSelected,
                                onSelect = {
                                    viewModel.selectSession(context, session.id)
                                    showHistoryDrawer = false
                                },
                                onRename = {
                                    renameTitleInput = session.title
                                    showRenameDialog = true
                                },
                                onDelete = {
                                    sessionToDeleteId = session.id
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Rename Session Dialog
    if (showRenameDialog && currentSession != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Conversation", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    label = { Text("Chat Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val session = currentSession
                        if (session != null && renameTitleInput.isNotBlank()) {
                            viewModel.renameSession(context, session.id, renameTitleInput)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Session Confirmation Dialog
    if (showDeleteDialog && sessionToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete Conversation?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this chat session and its message history.") },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToDeleteId?.let { viewModel.deleteSession(context, it) }
                        showDeleteDialog = false
                        sessionToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal dialog to edit LaTeX code and recompile
    editingMessage?.let { msg ->
        var editedCode by remember(msg.id) { mutableStateOf(msg.latexCode ?: "") }

        Dialog(onDismissRequest = { viewModel.stopEditingMessage() }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LaTeX Source Editor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Edit code and recompile to PDF in real-time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.stopEditingMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val quickSnippets = remember {
                        listOf(
                            "\\section{}" to "\\section{Title}\n",
                            "\\textbf{}" to "\\textbf{text}",
                            "\\frac{}{}" to "\\frac{a}{b}",
                            "\\begin{align}" to "\\begin{align}\n  \n\\end{align}",
                            "\\item" to "\\item ",
                            "\\sqrt{}" to "\\sqrt{x}",
                            "\\int" to "\\int_{0}^{1} ",
                            "\\begin{table}" to "\\begin{center}\n\\begin{tabular}{cc}\n\\toprule\nA & B \\\\\n\\midrule\n1 & 2 \\\\\n\\bottomrule\n\\end{tabular}\n\\end{center}"
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(quickSnippets) { (label, insertion) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.clickable {
                                    editedCode += insertion
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedCode,
                        onValueChange = { editedCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        placeholder = { Text("Enter LaTeX code here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${editedCode.lines().size} lines • ${editedCode.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (msg.latexCode != null && editedCode != msg.latexCode) {
                            TextButton(onClick = { editedCode = msg.latexCode ?: "" }) {
                                Text("Reset to Original", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.stopEditingMessage() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.recompileLatex(context, msg.id, editedCode)
                                viewModel.stopEditingMessage()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Recompile")
                        }
                    }
                }
            }
        }
    }

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                var showMenu by remember { mutableStateOf(false) }

                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                renameTitleInput = currentSession?.title ?: ""
                                showRenameDialog = true
                            }
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentSession?.title ?: "New Chat",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10172A),
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = Color(0xFF536580),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "dotPulse"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isGenerating) Color(0xFF246BFA).copy(alpha = pulseAlpha)
                                                else Color(0xFF22C55E).copy(alpha = pulseAlpha)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isGenerating) generatingStatus else "Online · Background Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF536580),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF10172A)
                            )
                        }
                    },
                    actions = {
                        // Open Chat History Sheet
                        IconButton(onClick = { showHistoryDrawer = true }) {
                            BadgedBox(
                                badge = {
                                    if (sessions.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color(0xFFE0E7FF),
                                            contentColor = Color(0xFF3730A3)
                                        ) {
                                            Text(
                                                text = sessions.size.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = Color(0xFF10172A)
                                )
                            }
                        }

                        // Start New Chat Button
                        IconButton(onClick = { viewModel.startNewChat(context) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = Color(0xFF10172A)
                            )
                        }

                        // More Actions
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color(0xFF10172A)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Chat") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        renameTitleInput = currentSession?.title ?: ""
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as Markdown") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        coroutineScope.launch {
                                            val md = viewModel.exportCurrentChatMarkdown(context)
                                            if (md.isNotBlank()) {
                                                shareText(context, md, "${currentSession?.title ?: "Chat"}.md")
                                            } else {
                                                Toast.makeText(context, "No messages to export", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        currentSession?.let {
                                            sessionToDeleteId = it.id
                                            showDeleteDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Background execution progress banner
                    if (isGenerating) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.70f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF246BFA)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = generatingStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10172A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "Running in Background",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF246BFA)
                                )
                            }
                        }
                    }

                    // Floating Frosted-Glass Prompt Container
                    Surface(
                        shape = RoundedCornerShape(36.dp),
                        color = Color.White.copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF10172A),
                                    fontSize = 14.5.sp
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "Ask for any LaTeX document, formula sheet...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF536580),
                                                fontSize = 14.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            val canSend = inputText.isNotBlank() && !isGenerating
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF2563EB),
                                                Color(0xFF4F46E5)
                                            )
                                        )
                                    )
                                    .clickable(enabled = canSend) {
                                        if (canSend) {
                                            val text = inputText
                                            inputText = ""
                                            viewModel.sendMessage(context, text)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (messages.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Translucent softly illuminated orb
                            IridescentOrb(size = 92.dp)

                            Spacer(modifier = Modifier.height(18.dp))

                            // Centered Title
                            Text(
                                text = "AI LaTeX to PDF Architect",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10172A),
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Balanced wrapped subtitle
                            Text(
                                text = "Create and edit LaTeX, fix syntax, and compile polished vector PDFs in foreground or background.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF536580),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .widthIn(max = 330.dp)
                                    .padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Text-only feature chips
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val minW = maxWidth
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .widthIn(min = minW),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FeaturePill(
                                        text = "Instant Background Worker",
                                        containerColor = Color(0xFFDCEBFF).copy(alpha = 0.70f),
                                        borderColor = Color(0xFF93C5FD).copy(alpha = 0.85f),
                                        textColor = Color(0xFF1D4ED8)
                                    )
                                    FeaturePill(
                                        text = "Live Code Editor",
                                        containerColor = Color(0xFFEADBFF).copy(alpha = 0.70f),
                                        borderColor = Color(0xFFC084FC).copy(alpha = 0.85f),
                                        textColor = Color(0xFF6B21A8)
                                    )
                                    FeaturePill(
                                        text = "Multi-Chat History",
                                        containerColor = Color(0xFFE2E8F0).copy(alpha = 0.70f),
                                        borderColor = Color(0xFFCBD5E1).copy(alpha = 0.90f),
                                        textColor = Color(0xFF334155)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Category Tabs in a single horizontal row
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = Color.White.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val categories = viewModel.promptCategories
                                    categories.forEachIndexed { idx, cat ->
                                        val isSelected = idx == selectedCategoryIndex
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(26.dp))
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    Color(0xFF246BFA),
                                                                    Color(0xFF00B0FF)
                                                                )
                                                            )
                                                        )
                                                    } else {
                                                        Modifier.background(Color.Transparent)
                                                    }
                                                )
                                                .clickable { viewModel.selectCategory(idx) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cat.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF10172A),
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val activeCategory = viewModel.promptCategories.getOrNull(selectedCategoryIndex)
                    if (activeCategory != null) {
                        items(activeCategory.prompts, key = { it.title }) { promptItem: PromptItem ->
                            GlassPromptSuggestionCard(
                                item = promptItem,
                                onClick = {
                                    viewModel.sendMessage(context, promptItem.promptText)
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                ) {
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                        if (msg.isUser) {
                            UserChatBubble(msg)
                        } else {
                            BotChatBubble(
                                message = msg,
                                isGeneratingThis = isGenerating && msg.id == messages.lastOrNull()?.id,
                                generatingStatus = generatingStatus,
                                onEditLatex = { viewModel.startEditingMessage(msg) },
                                onRecompile = {
                                    msg.latexCode?.let { code ->
                                        viewModel.recompileLatex(context, msg.id, code)
                                    }
                                },
                                onRetry = {
                                    viewModel.retryGeneration(context, msg.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItemRow(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(session.lastUpdatedAt) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(session.lastUpdatedAt))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Chat else Icons.Default.History,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.previewText.ifBlank { dateStr },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun IridescentOrb(
    modifier: Modifier = Modifier,
    size: Dp = 92.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF818CF8).copy(alpha = 0.35f),
                spotColor = Color(0xFFA78BFA).copy(alpha = 0.45f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            val r = this.size.minDimension / 2f
            val centerOffset = Offset(r, r)

            // 1. Base translucent iridescent radial gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2F4FF).copy(alpha = 0.95f),
                        Color(0xFFB8DBFD).copy(alpha = 0.85f),
                        Color(0xFFCDBEFE).copy(alpha = 0.80f),
                        Color(0xFFF3BFE8).copy(alpha = 0.75f)
                    ),
                    center = Offset(r * 0.75f, r * 0.65f),
                    radius = r * 1.05f
                ),
                radius = r,
                center = centerOffset
            )

            // 2. Soft violet/pink bloom on bottom right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE9D5FF).copy(alpha = 0.60f),
                        Color.Transparent
                    ),
                    center = Offset(r * 1.35f, r * 1.35f),
                    radius = r * 0.75f
                ),
                radius = r,
                center = centerOffset
            )

            // 3. Specular upper crescent highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.90f),
                        Color.White.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(r * 0.65f, r * 0.42f),
                    radius = r * 0.48f
                ),
                radius = r * 0.46f,
                center = Offset(r * 0.65f, r * 0.42f)
            )

            // 4. Subtle frosted outer rim stroke
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = r - 0.75.dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun FeaturePill(
    text: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 12.5.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun GlassPromptSuggestionCard(
    item: PromptItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle ambient wave flare on right side of the card matching the mockup
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                val w = size.width
                val h = size.height

                // Gentle translucent wave wash
                val wavePath = Path().apply {
                    moveTo(w * 0.55f, h)
                    cubicTo(
                        w * 0.68f, h * 0.82f,
                        w * 0.74f, h * 0.35f,
                        w, h * 0.12f
                    )
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFFCEEAFF).copy(alpha = 0.28f)
                        ),
                        start = Offset(w * 0.60f, h),
                        end = Offset(w, 0f)
                    )
                )

                // Thin white wave stroke
                val strokePath = Path().apply {
                    moveTo(w * 0.55f, h)
                    cubicTo(
                        w * 0.68f, h * 0.82f,
                        w * 0.74f, h * 0.35f,
                        w, h * 0.12f
                    )
                }
                drawPath(
                    path = strokePath,
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10172A),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF536580),
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Circular chevron button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE5F0FC).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserChatBubble(message: ChatMessage) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp,
                modifier = Modifier.clickable {
                    clipboard.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Copied prompt to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun BotChatBubble(
    message: ChatMessage,
    isGeneratingThis: Boolean,
    generatingStatus: String,
    onEditLatex: () -> Unit,
    onRecompile: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var isCodeExpanded by remember { mutableStateOf(false) }

    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Bot Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DocMorph AI",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val fullContent = "${message.text}\n\n${message.latexCode ?: ""}".trim()
                                    clipboard.setText(AnnotatedString(fullContent))
                                    Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Response",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isGeneratingThis) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = generatingStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // LaTeX Code Card
                    if (!message.latexCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { isCodeExpanded = !isCodeExpanded }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "LaTeX",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${message.latexCode.lines().size} lines",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isCodeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(message.latexCode))
                                                Toast.makeText(context, "LaTeX code copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy LaTeX",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = onEditLatex,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit LaTeX",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = isCodeExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0F141C),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = message.latexCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = Color(0xFF81D4FA),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PDF Output Presentation Card
                    when (message.pdfStatus) {
                        ChatPdfStatus.COMPILING -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Compiling to PDF...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "XeLaTeX compiler is generating vector pages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        ChatPdfStatus.READY -> {
                            val file = message.pdfFile
                            if (file != null && file.exists()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFD32F2F),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.PictureAsPdf,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = message.pdfTitle ?: file.nameWithoutExtension,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${FileUtils.formatFileSize(file.length())} • Vector PDF ready",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    FileUtils.openPdf(context, file, "Open Document")
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open PDF")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    sharePdf(context, file)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ChatPdfStatus.ERROR -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Compilation notice",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.errorMessage ?: "Could not compile PDF.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onRecompile) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry Compilation")
                                        }
                                        TextButton(onClick = onRetry) {
                                            Text("Regenerate")
                                        }
                                    }
                                }
                            }
                        }
                        ChatPdfStatus.IDLE -> {}
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareText(context: android.content.Context, text: String, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Conversation"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
