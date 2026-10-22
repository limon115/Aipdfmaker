package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.DefaultPrompts
import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassButton
import com.example.ui.components.glass.GlassCard
import com.example.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPromptsConfigScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val settings = settingsNullable
    var selectedTab by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val savedNotePrompt = settings?.promptNoteGeneration ?: DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT
    val savedMathPrompt = settings?.promptMathSolver ?: DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT
    val savedChatPrompt = settings?.promptLatexChat ?: DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT

    var notePromptText by remember(savedNotePrompt) { mutableStateOf(savedNotePrompt) }
    var mathPromptText by remember(savedMathPrompt) { mutableStateOf(savedMathPrompt) }
    var chatPromptText by remember(savedChatPrompt) { mutableStateOf(savedChatPrompt) }

    val currentText = when (selectedTab) {
        0 -> notePromptText
        1 -> mathPromptText
        else -> chatPromptText
    }

    val currentTitle = when (selectedTab) {
        0 -> "Note Generator Prompt"
        1 -> "Math Solver Prompt"
        else -> "LaTeX AI Assistant Prompt"
    }

    val currentSubtitle = when (selectedTab) {
        0 -> "System instructions and LaTeX pedagogical formatting rules sent to AI #2 for generating topic notes."
        1 -> "Prompt sent to the AI Math Tutor when solving problems and compiling step-by-step PDF solutions."
        else -> "System prompt powering the DocMorph LaTeX & PDF AI Expert Assistant interactive chat."
    }

    val currentPlaceholders = when (selectedTab) {
        0 -> listOf("${'$'}topicTitle" to "The name of the current topic/chapter to write notes for")
        1 -> listOf("${'$'}problemText" to "The user-entered mathematical problem or equation")
        else -> listOf(
            "${'$'}historyContext" to "Recent conversation history context snippet",
            "${'$'}userPrompt" to "The current user message or request"
        )
    }

    val currentAccentColor = when (selectedTab) {
        0 -> Color(0xFF6366F1) // Indigo
        1 -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF8B5CF6) // Purple
    }

    val currentIcon: ImageVector = when (selectedTab) {
        0 -> Icons.Default.Description
        1 -> Icons.Default.Functions
        else -> Icons.Default.Terminal
    }

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "AI Prompts & Templates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showResetDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset all prompts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Customize the raw system prompts and formatting guidelines dispatched to AI models across all features of the application.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Feedback banner
                AnimatedVisibility(
                    visible = saveFeedbackMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    saveFeedbackMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Prompt Category Selector Tabs
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Math", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Selected Prompt Overview Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(currentAccentColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentIcon,
                                    contentDescription = null,
                                    tint = currentAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Available Placeholders
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "AVAILABLE PLACEHOLDERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentPlaceholders.forEach { (tag, desc) ->
                                    SuggestionChip(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(tag))
                                            Toast.makeText(context, "Copied $tag", Toast.LENGTH_SHORT).show()
                                        },
                                        label = {
                                            Text(
                                                text = tag,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy tag",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }

                            currentPlaceholders.forEach { (tag, desc) ->
                                Text(
                                    text = "• $tag: $desc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Prompt Editor Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROMPT TEMPLATE CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = "${currentText.length} chars • ${currentText.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { updated ->
                                when (selectedTab) {
                                    0 -> notePromptText = updated
                                    1 -> mathPromptText = updated
                                    else -> chatPromptText = updated
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 340.dp, max = 560.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp
                            ),
                            placeholder = {
                                Text(
                                    "Enter custom system prompt guidelines...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                focusedBorderColor = currentAccentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )

                        // Action Buttons for current tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    when (selectedTab) {
                                        0 -> {
                                            notePromptText = DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT
                                            viewModel.updatePromptNoteGeneration(DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT)
                                            saveFeedbackMessage = "Reset Note Generator prompt to factory default"
                                        }
                                        1 -> {
                                            mathPromptText = DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT
                                            viewModel.updatePromptMathSolver(DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT)
                                            saveFeedbackMessage = "Reset Math Solver prompt to factory default"
                                        }
                                        else -> {
                                            chatPromptText = DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT
                                            viewModel.updatePromptLatexChat(DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT)
                                            saveFeedbackMessage = "Reset LaTeX Assistant prompt to factory default"
                                        }
                                    }
                                    Toast.makeText(context, "Prompt restored to default", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Default", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    when (selectedTab) {
                                        0 -> {
                                            viewModel.updatePromptNoteGeneration(notePromptText)
                                            saveFeedbackMessage = "Note Generator prompt saved successfully"
                                        }
                                        1 -> {
                                            viewModel.updatePromptMathSolver(mathPromptText)
                                            saveFeedbackMessage = "Math Solver prompt saved successfully"
                                        }
                                        else -> {
                                            viewModel.updatePromptLatexChat(chatPromptText)
                                            saveFeedbackMessage = "LaTeX Assistant prompt saved successfully"
                                        }
                                    }
                                    Toast.makeText(context, "Saved prompt changes", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentAccentColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Changes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Global Prompt Instructions Info Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "How Prompt Injection Works",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "When Note Generation, AI Math Solver, or LaTeX Chat executes, the system dynamically injects your configured prompt from this screen. The XeLaTeX compilation engine will receive and compile the resulting output strictly according to the rules in your active template.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Reset All Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Reset All Prompts to Default?")
            },
            text = {
                Text("This will restore the Note Generator, Math Solver, and LaTeX AI Assistant prompts to their factory default templates. Any custom modifications will be replaced.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPromptsToDefault()
                        notePromptText = DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT
                        mathPromptText = DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT
                        chatPromptText = DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT
                        showResetDialog = false
                        saveFeedbackMessage = "All prompts have been reset to factory defaults"
                        Toast.makeText(context, "All prompts reset to default", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
