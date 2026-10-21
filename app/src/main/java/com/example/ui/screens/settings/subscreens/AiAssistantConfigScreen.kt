package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.AiProvider
import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassButton
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassTextField
import com.example.ui.screens.settings.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantConfigScreen(
    aiIndex: Int,
    viewModel: SettingsViewModel,
    onNavigateToProviderSelection: (aiIndex: Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val title = when (aiIndex) {
        1 -> "AI #1 – Blueprint Builder"
        2 -> "AI #2 – Note Generator"
        else -> "AI #3 – LaTeX Debugger"
    }

    val subtitle = when (aiIndex) {
        1 -> "Extracts document hierarchies, conceptual themes, and structured outlines from inputs."
        2 -> "Drafts comprehensive, publication-grade LaTeX formulas, diagrams, and detailed notes."
        else -> "Diagnoses compilation logs, repairs broken math blocks, and fixes missing TeX packages."
    }

    val accentColor = when (aiIndex) {
        1 -> Color(0xFFF59E0B) // Amber
        2 -> Color(0xFF6366F1) // Indigo
        else -> Color(0xFF14B8A6) // Teal
    }

    val slotIcon: ImageVector = when (aiIndex) {
        1 -> Icons.Default.AutoAwesome
        2 -> Icons.Default.Description
        else -> Icons.Default.BugReport
    }

    val settings = settingsNullable

    val currentProvider = when (aiIndex) {
        1 -> settings?.ai1Provider ?: AiProvider.GOOGLE_GEMINI
        2 -> settings?.ai2Provider ?: AiProvider.GOOGLE_GEMINI
        else -> settings?.ai3Provider ?: AiProvider.GOOGLE_GEMINI
    }

    val currentModel = when (aiIndex) {
        1 -> settings?.ai1Model ?: ""
        2 -> settings?.ai2Model ?: ""
        else -> settings?.ai3Model ?: ""
    }

    val currentApiKey = when (aiIndex) {
        1 -> settings?.ai1ApiKey ?: ""
        2 -> settings?.ai2ApiKey ?: ""
        else -> settings?.ai3ApiKey ?: ""
    }

    val temperature = when (aiIndex) {
        1 -> settings?.ai1Temperature ?: 0.5f
        2 -> settings?.ai2Temperature ?: 0.7f
        else -> settings?.ai3Temperature ?: 0.2f
    }

    val maxTokens = when (aiIndex) {
        1 -> settings?.ai1MaxTokens ?: 2048
        2 -> settings?.ai2MaxTokens ?: 4096
        else -> settings?.ai3MaxTokens ?: 2048
    }

    val topP = when (aiIndex) {
        1 -> settings?.ai1TopP ?: 0.95f
        2 -> settings?.ai2TopP ?: 1.0f
        else -> settings?.ai3TopP ?: 0.9f
    }

    var localApiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var localModel by remember(currentModel) { mutableStateOf(currentModel) }
    var passwordVisible by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }

    val recommendedModels = when (currentProvider) {
        AiProvider.GOOGLE_GEMINI -> listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-pro-exp", "gemini-1.5-flash-8b")
        AiProvider.OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "o1", "o1-mini")
        AiProvider.ANTHROPIC_CLAUDE -> listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        AiProvider.OPENROUTER -> listOf("deepseek/deepseek-r1", "deepseek/deepseek-chat", "anthropic/claude-3.7-sonnet", "google/gemini-2.0-flash", "meta-llama/llama-3.3-70b-instruct", "openai/o3-mini")
        AiProvider.LM_STUDIO -> listOf("deepseek-r1", "llama-3.3-70b-instruct", "phi-4", "qwen-2.5-coder")
        AiProvider.OLLAMA -> listOf("deepseek-r1", "llama3.3", "phi4", "qwen2.5", "gemma2")
        else -> listOf("default-model")
    }

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Hero Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slotIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Provider Selection Card
            Text(
                text = "PROVIDER CONFIGURATION",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            GlassCard(
                onClick = { onNavigateToProviderSelection(aiIndex) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Provider",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentProvider.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Provider",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Model Selection
            Text(
                text = "MODEL & CREDENTIALS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Model Dropdown
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassTextField(
                            value = localModel,
                            onValueChange = {
                                localModel = it
                                when (aiIndex) {
                                    1 -> viewModel.updateAi1Model(it)
                                    2 -> viewModel.updateAi2Model(it)
                                    else -> viewModel.updateAi3Model(it)
                                }
                            },
                            label = { Text("Model Name (e.g. ${recommendedModels.firstOrNull() ?: "model"})") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            recommendedModels.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        localModel = option
                                        when (aiIndex) {
                                            1 -> viewModel.updateAi1Model(option)
                                            2 -> viewModel.updateAi2Model(option)
                                            else -> viewModel.updateAi3Model(option)
                                        }
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // API Key Field
                    GlassTextField(
                        value = localApiKey,
                        onValueChange = {
                            localApiKey = it
                            when (aiIndex) {
                                1 -> viewModel.updateAi1ApiKey(it)
                                2 -> viewModel.updateAi2ApiKey(it)
                                else -> viewModel.updateAi3ApiKey(it)
                            }
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val clipData = clipboardManager.getText()?.text
                                    if (!clipData.isNullOrBlank()) {
                                        localApiKey = clipData.trim()
                                        when (aiIndex) {
                                            1 -> viewModel.updateAi1ApiKey(localApiKey)
                                            2 -> viewModel.updateAi2ApiKey(localApiKey)
                                            else -> viewModel.updateAi3ApiKey(localApiKey)
                                        }
                                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste API Key",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide Key" else "Show Key",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )

                    // Test Connection Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (localApiKey.isNotBlank()) "Key configured" else "No key entered",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (localApiKey.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                isTesting = true
                                viewModel.testConnection(
                                    provider = currentProvider.name,
                                    model = localModel,
                                    apiKey = localApiKey,
                                    onSuccess = {
                                        isTesting = false
                                        Toast.makeText(context, "Connection Successful!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        isTesting = false
                                        clipboardManager.setText(AnnotatedString(err))
                                        Toast.makeText(context, "Connection Failed: Error copied to clipboard", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing...")
                            } else {
                                Text("Test Connection")
                            }
                        }
                    }
                }
            }

            // Assistant Parameters Section
            Text(
                text = "MODEL PARAMETERS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Temperature
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temperature", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(String.format(Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { newTemp ->
                                when (aiIndex) {
                                    1 -> viewModel.updateAi1Advanced(newTemp, maxTokens, topP)
                                    2 -> viewModel.updateAi2Advanced(newTemp, maxTokens, topP)
                                    else -> viewModel.updateAi3Advanced(newTemp, maxTokens, topP)
                                }
                            },
                            valueRange = 0f..2f
                        )
                        Text(
                            text = when (aiIndex) {
                                1 -> "Lower temperature (0.3-0.5) ensures consistent document structure outlines."
                                2 -> "Balanced temperature (0.6-0.8) yields creative proofs with accurate LaTeX."
                                else -> "Low temperature (0.1-0.3) is critical for deterministic compiler bug fixing."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Top P
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Top P (Nucleus Sampling)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(String.format(Locale.US, "%.2f", topP), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = topP,
                            onValueChange = { newTopP ->
                                when (aiIndex) {
                                    1 -> viewModel.updateAi1Advanced(temperature, maxTokens, newTopP)
                                    2 -> viewModel.updateAi2Advanced(temperature, maxTokens, newTopP)
                                    else -> viewModel.updateAi3Advanced(temperature, maxTokens, newTopP)
                                }
                            },
                            valueRange = 0f..1f
                        )
                    }

                    // Max Tokens
                    GlassTextField(
                        value = maxTokens.toString(),
                        onValueChange = { input ->
                            input.toIntOrNull()?.let { tokens ->
                                when (aiIndex) {
                                    1 -> viewModel.updateAi1Advanced(temperature, tokens, topP)
                                    2 -> viewModel.updateAi2Advanced(temperature, tokens, topP)
                                    else -> viewModel.updateAi3Advanced(temperature, tokens, topP)
                                }
                            }
                        },
                        label = { Text("Max Output Tokens") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Role Description Callout
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = when (aiIndex) {
                            1 -> "Slot #1 parses incoming PDFs, transcripts, and YouTube transcripts, producing the structured topic graph."
                            2 -> "Slot #2 generates the complete TeX documents, rendering multi-column equations and tikz graphics."
                            else -> "Slot #3 receives TeX compiler stdout/stderr logs and executes automated regex & AST patch passes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}
