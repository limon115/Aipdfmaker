package com.example.ui.screens.settings

import com.example.ui.components.glass.GlassTextField


import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.MenuAnchorType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.datastore.AiSettings
import com.example.domain.models.AiProvider
import com.example.domain.models.ThemeMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToProviderSelection: (aiIndex: Int) -> Unit,
    onNavigateToApiLab: () -> Unit,
    onNavigateToLogs: () -> Unit = {}
) {
    val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        if (settingsNullable == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val settings = settingsNullable!!
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSettingsCard(
                themeMode = settings.themeMode,
                onThemeModeChange = { viewModel.updateThemeMode(it) },
                customFontPath = settings.customFontPath,
                onFontPicked = { uri -> viewModel.saveCustomFont(uri) },
                onClearFont = { viewModel.clearCustomFont() }
            )
            AiConfigCard(
                title = "AI #1 - Blueprint Builder",
                provider = settings.ai1Provider,
                model = settings.ai1Model,
                apiKey = settings.ai1ApiKey,
                onProviderClick = { onNavigateToProviderSelection(1) },
                onModelChange = viewModel::updateAi1Model,
                onApiKeyChange = viewModel::updateAi1ApiKey,
                onTestConnection = { provider, model, apiKey ->
                    viewModel.testConnection(
                        provider = provider,
                        model = model,
                        apiKey = apiKey,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                            android.widget.Toast.makeText(context, "Connection Failed: copied full error to clipboard", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )

            AiConfigCard(
                title = "AI #2 - Note Generator",
                provider = settings.ai2Provider,
                model = settings.ai2Model,
                apiKey = settings.ai2ApiKey,
                onProviderClick = { onNavigateToProviderSelection(2) },
                onModelChange = viewModel::updateAi2Model,
                onApiKeyChange = viewModel::updateAi2ApiKey,
                onTestConnection = { provider, model, apiKey ->
                    viewModel.testConnection(
                        provider = provider,
                        model = model,
                        apiKey = apiKey,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                            android.widget.Toast.makeText(context, "Connection Failed: copied full error to clipboard", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                },
                advancedSettings = {
                    AdvancedSettingsSection(
                        temperature = settings.ai2Temperature,
                        maxTokens = settings.ai2MaxTokens,
                        topP = settings.ai2TopP,
                        onTemperatureChange = { viewModel.updateAi2Advanced(it, settings.ai2MaxTokens, settings.ai2TopP) },
                        onMaxTokensChange = { viewModel.updateAi2Advanced(settings.ai2Temperature, it, settings.ai2TopP) },
                        onTopPChange = { viewModel.updateAi2Advanced(settings.ai2Temperature, settings.ai2MaxTokens, it) }
                    )
                }
            )

            AiConfigCard(
                title = "AI #3 - LaTeX Debugger",
                provider = settings.ai3Provider,
                model = settings.ai3Model,
                apiKey = settings.ai3ApiKey,
                onProviderClick = { onNavigateToProviderSelection(3) },
                onModelChange = viewModel::updateAi3Model,
                onApiKeyChange = viewModel::updateAi3ApiKey,
                onTestConnection = { provider, model, apiKey ->
                    viewModel.testConnection(
                        provider = provider,
                        model = model,
                        apiKey = apiKey,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                            android.widget.Toast.makeText(context, "Connection Failed: copied full error to clipboard", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )

            
            BatteryOptimizationCard()
            
            // Developer Tools Section
            DeveloperToolsCard(
                onNavigateToApiLab = onNavigateToApiLab,
                onNavigateToLogs = onNavigateToLogs
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigCard(
    title: String,
    provider: AiProvider,
    model: String,
    apiKey: String,
    onProviderClick: () -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: (provider: String, model: String, apiKey: String) -> Unit,
    advancedSettings: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Decouple UI state from DataStore to prevent async cursor jumps
    var localApiKey by remember { mutableStateOf(apiKey) }
    
    var localModel by remember { mutableStateOf(model) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val recommendedModels = when (provider) {
        AiProvider.GOOGLE_GEMINI -> listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-pro-exp", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.5-flash-8b")
        AiProvider.OPENAI -> listOf("o1", "o3-mini", "o1-mini", "gpt-4.5-preview", "gpt-4o", "gpt-4o-mini")
        AiProvider.ANTHROPIC_CLAUDE -> listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        AiProvider.OPENROUTER -> listOf("deepseek/deepseek-r1", "deepseek/deepseek-chat", "anthropic/claude-3.7-sonnet", "google/gemini-2.5-pro", "meta-llama/llama-3.3-70b-instruct", "openai/o3-mini")
        AiProvider.LM_STUDIO -> listOf("deepseek-r1", "llama-3.3-70b-instruct", "phi-4", "qwen-2.5-coder")
        AiProvider.OLLAMA -> listOf("deepseek-r1", "llama3.3", "phi4", "qwen2.5", "gemma2")
        else -> listOf("default-model")
    }

    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Provider Selection
            Column {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                com.example.ui.components.glass.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onProviderClick),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Provider"
                        )
                    }
                }
            }

            // Model Selection
            ExposedDropdownMenuBox(
                expanded = modelDropdownExpanded,
                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassTextField(
                    value = localModel,
                    onValueChange = { localModel = it; onModelChange(it) },
                    label = { Text("Model (e.g. ${recommendedModels.first()})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                    },
                )
                
                ExposedDropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false }
                ) {
                    recommendedModels.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onModelChange(option)
                                modelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // API Key
            GlassTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it; onApiKeyChange(it) },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide API Key" else "Show API Key"
                            )
                        }
                    }
                }
            )

            var isTesting by remember { mutableStateOf(false) }

            TextButton(
                onClick = { 
                    isTesting = true
                    onTestConnection(provider.name, localModel, localApiKey)
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Test Connection", color = MaterialTheme.colorScheme.primary)
                }
            }

            LaunchedEffect(isTesting) {
                if (isTesting) {
                    kotlinx.coroutines.delay(2000)
                    isTesting = false
                }
            }

            if (advancedSettings != null) {
                advancedSettings()
            }
        }
    }
}

@Composable
fun AdvancedSettingsSection(
    temperature: Float,
    maxTokens: Int,
    topP: Float,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onTopPChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Advanced Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Toggle Advanced Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature", style = MaterialTheme.typography.bodyMedium)
                        Text(String.format(Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = temperature,
                        onValueChange = onTemperatureChange,
                        valueRange = 0f..2f
                    )
                }
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Top P", style = MaterialTheme.typography.bodyMedium)
                        Text(String.format(Locale.US, "%.2f", topP), style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = topP,
                        onValueChange = onTopPChange,
                        valueRange = 0f..1f
                    )
                }

                GlassTextField(
                    value = maxTokens.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { tokens -> onMaxTokensChange(tokens) }
                    },
                    label = { Text("Max Output Tokens") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName
    
    // Using a key that changes if the user returns to the screen could be nice, but simple remember is okay 
    // for this settings screen, or we can just calculate it in real time during composition.
    val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
    
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Processing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "To ensure long document generations are not killed by the system, please disable battery optimization for this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isIgnoringOptimizations) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Battery optimization is disabled (Recommended)", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent()
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val alternateIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try {
                                context.startActivity(alternateIntent)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Disable Optimization")
                }
            }
        }
    }
}

@Composable
fun AiUsageDashboardCard() {
    val stats by com.example.domain.services.ai.AiUsageTracker.stats.collectAsStateWithLifecycle()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.example.ui.components.glass.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Session AI Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Requests:", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.requests}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Est. Tokens Used:", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.estimatedTokens}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cache Hits (Saved Tokens):", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.cacheHits}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rate Limit (429) Errors:", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.rateLimitErrors}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (stats.rateLimitErrors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        if (stats.tokensByFeature.isNotEmpty()) {
            UsagePieChartCard(stats.tokensByFeature, "Token Distribution")
        }
        
        if (stats.requestsByFeature.isNotEmpty()) {
            FeatureBarChartCard(stats.requestsByFeature, "Requests by Feature")
        }
        
        UsageBarChartCard(stats.requests, stats.cacheHits, stats.rateLimitErrors)
    }
}

@Composable
fun UsagePieChartCard(data: Map<String, Int>, title: String) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val total = data.values.sum().coerceAtLeast(1)
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.error,
                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                androidx.compose.ui.graphics.Color(0xFFFF9800)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pie Chart Canvas
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp).padding(8.dp)) {
                    var startAngle = -90f
                    data.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value.toFloat() / total) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size
                        )
                        startAngle += sweepAngle
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.entries.forEachIndexed { index, entry ->
                        val percentage = (entry.value.toFloat() / total) * 100
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size], androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(entry.key, style = MaterialTheme.typography.labelMedium)
                                Text("${entry.value} (${String.format("%.1f", percentage)}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBarChartCard(data: Map<String, Int>, title: String) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.entries.forEachIndexed { index, entry ->
                    ChartBarRow(entry.key, entry.value, maxVal, colors[index % colors.size])
                }
            }
        }
    }
}

@Composable
fun UsageBarChartCard(requests: Int, cacheHits: Int, rateLimits: Int) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Usage Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val maxVal = maxOf(requests, cacheHits, rateLimits, 1) // Avoid div by zero
            
            val primaryColor = MaterialTheme.colorScheme.primary
            val successColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            val errorColor = MaterialTheme.colorScheme.error
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartBarRow("Requests", requests, maxVal, primaryColor)
                ChartBarRow("Cache Hits", cacheHits, maxVal, successColor)
                ChartBarRow("Rate Limits", rateLimits, maxVal, errorColor)
            }
        }
    }
}

@Composable
fun ChartBarRow(label: String, value: Int, maxVal: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(4.dp))
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (value > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp).align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun DeveloperToolsCard(
    onNavigateToApiLab: () -> Unit,
    onNavigateToLogs: () -> Unit = {}
) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Developer Tools",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            ListItem(
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                headlineContent = { Text("API Lab") },
                supportingContent = { Text("Test and diagnose your AI connection") },
                leadingContent = {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToApiLab() },
            )
            
            ListItem(
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                headlineContent = { Text("App Logs") },
                supportingContent = { Text("View internal application logs for debugging") },
                leadingContent = {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLogs() },
            )
        }
    }
}

@Composable
fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    customFontPath: String = "",
    onFontPicked: (android.net.Uri) -> Unit = {},
    onClearFont: () -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onFontPicked(uri)
        }
    }

    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ThemeOptionButton(
                    text = "System",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ThemeOptionButton(
                    text = "Light",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ThemeOptionButton(
                    text = "Dark",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Custom App Font", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pick a .ttf or .otf file from your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Text("Pick Font")
                }
                
                if (customFontPath.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearFont,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor,
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
