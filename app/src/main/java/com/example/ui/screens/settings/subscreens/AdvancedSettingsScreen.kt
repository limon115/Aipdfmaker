package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassTextField
import com.example.ui.screens.settings.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val settings = settingsNullable
    val temperature = settings?.ai2Temperature ?: 0.7f
    val maxTokens = settings?.ai2MaxTokens ?: 2048
    val topP = settings?.ai2TopP ?: 1.0f
    val isStreaming = settings?.streamingEnabled ?: true
    val isCaching = settings?.cacheEnabled ?: true
    val isPruning = settings?.contextPruningEnabled ?: true
    val systemPrompt = settings?.systemPrompt ?: ""

    var localPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Advanced Settings",
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
            Text(
                text = "Fine-tune generation hyperparameters, token streaming, context memory, and global model instructions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Generation Hyperparameters Card
            Text(
                text = "GLOBAL GENERATION HYPERPARAMETERS",
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
                            Text("Sampling Temperature", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { viewModel.updateAi2Advanced(it, maxTokens, topP) },
                            valueRange = 0f..2f
                        )
                        Text(
                            text = "Controls randomness: 0.0 is completely deterministic; 1.0+ enables creative derivations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Top-P Nucleus Sampling
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Top P (Nucleus Sampling)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.US, "%.2f", topP), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = topP,
                            onValueChange = { viewModel.updateAi2Advanced(temperature, maxTokens, it) },
                            valueRange = 0f..1f
                        )
                        Text(
                            text = "Cumulative probability cutoff for token candidate selection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Max Output Tokens
                    GlassTextField(
                        value = maxTokens.toString(),
                        onValueChange = { input ->
                            input.toIntOrNull()?.let { tokens ->
                                viewModel.updateAi2Advanced(temperature, tokens, topP)
                            }
                        },
                        label = { Text("Max Output Tokens per Request") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Pipeline Optimization Toggles
            Text(
                text = "PIPELINE OPTIMIZATIONS",
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
                    // Streaming toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Stream, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Stream Responses", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Render words progressively in LaTeX chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isStreaming,
                            onCheckedChange = { viewModel.updateStreamingEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Caching toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Cached, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Response Caching", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Reuse cached results to save tokens & prevent 429s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isCaching,
                            onCheckedChange = { viewModel.updateCacheEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Context pruning
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Smart Context Pruning", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Compress conversation history on large files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isPruning,
                            onCheckedChange = { viewModel.updateContextPruningEnabled(it) }
                        )
                    }
                }
            }

            // Global System Prompt Override
            Text(
                text = "GLOBAL SYSTEM PROMPT OVERRIDE",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Inject custom rules or style instructions into all outgoing AI prompts across Blueprint, Note Generation, and Chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GlassTextField(
                        value = localPrompt,
                        onValueChange = {
                            localPrompt = it
                            viewModel.updateSystemPrompt(it)
                        },
                        placeholder = { Text("e.g. Always format mathematical proofs in LaTeX align* environments. Include intuitive step-by-step physical explanations.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        singleLine = false,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (localPrompt.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    localPrompt = ""
                                    viewModel.updateSystemPrompt("")
                                    Toast.makeText(context, "System prompt cleared", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Prompt")
                            }
                        }
                    }
                }
            }

            // Reset Hyperparameters Button
            OutlinedButton(
                onClick = {
                    viewModel.updateAi2Advanced(0.7f, 2048, 1.0f)
                    viewModel.updateStreamingEnabled(true)
                    viewModel.updateCacheEnabled(true)
                    viewModel.updateContextPruningEnabled(true)
                    Toast.makeText(context, "Reset all generation settings to defaults", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Recommended Defaults")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}
