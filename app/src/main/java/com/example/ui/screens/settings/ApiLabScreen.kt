package com.example.ui.screens.settings

import com.example.ui.components.glass.GlassTextField


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.services.ai.GeminiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLabScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApiLabViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf<GeminiModel?>(null) }
    var testPrompt by remember { mutableStateOf("Say \"API test successful\".") }
    
    val apiKeyTestState by viewModel.apiKeyTestState.collectAsStateWithLifecycle()
    val modelTestState by viewModel.modelTestState.collectAsStateWithLifecycle()
    
    // Auto-select first model when models are loaded
    LaunchedEffect(apiKeyTestState) {
        if (apiKeyTestState is TestState.Success) {
            val models = (apiKeyTestState as TestState.Success).models
            if (models.isNotEmpty() && selectedModel == null) {
                selectedModel = models.first()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Test and diagnose your AI connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Provider (Static for now as per requirements)
            ProviderSelection()

            // API Key Test
            ApiKeyTestSection(
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it; viewModel.resetTestState(); selectedModel = null },
                testState = apiKeyTestState,
                onTestClick = { viewModel.testApiKey(apiKey) }
            )

            // Available Models & Model Test (Only show if API Key is valid)
            AnimatedVisibility(visible = apiKeyTestState is TestState.Success) {
                val models = (apiKeyTestState as TestState.Success?)?.models ?: emptyList()
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AvailableModelsSection(models)
                    
                    ModelTestSection(
                        models = models,
                        selectedModel = selectedModel,
                        onModelSelected = { selectedModel = it; viewModel.resetModelTestState() },
                        prompt = testPrompt,
                        onPromptChange = { testPrompt = it },
                        testState = modelTestState,
                        onTestClick = { 
                            selectedModel?.let { model ->
                                viewModel.testModel(apiKey, model.name, testPrompt)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderSelection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Provider", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassTextField(
            value = "✦ Google Gemini",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
            shape = RoundedCornerShape(8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("AI Configuration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = { })
                Text("AI 1")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = { })
                Text("AI 2")
            }
        }
    }
}

@Composable
fun ApiKeyTestSection(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    testState: TestState,
    onTestClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("API KEY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("AIza••••••••••••••••") }
        )
        
        Button(
            onClick = onTestClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank() && testState !is TestState.Testing,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Test API Key")
        }
        
        when (testState) {
            is TestState.Testing -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing API connection...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is TestState.Success -> {
                com.example.ui.components.glass.GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isDark = com.example.ui.theme.AppTheme.colors.isDark
                            val successColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            val successColorMuted = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Success", tint = successColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("API KEY VALID", fontWeight = FontWeight.Bold, color = successColor)
                        }
                        Text("Connection successful", style = MaterialTheme.typography.bodyMedium, color = successColor)
                        Text("Response time    ${testState.latencyMs} ms", style = MaterialTheme.typography.bodySmall, color = successColor)
                        Text("Models found     ${testState.models.size}", style = MaterialTheme.typography.bodySmall, color = successColor)
                    }
                }
            }
            is TestState.Error -> {
                ErrorDiagnosticsCard(testState.code, testState.message, onRetry = onTestClick)
            }
            else -> {}
        }
    }
}

@Composable
fun AvailableModelsSection(models: List<GeminiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AVAILABLE MODELS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${models.size} generation models", style = MaterialTheme.typography.bodyMedium)
        
        val exampleModel = models.firstOrNull()
        if (exampleModel != null) {
            com.example.ui.components.glass.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exampleModel.displayName.ifEmpty { exampleModel.name }, fontWeight = FontWeight.Bold)
                    Text(exampleModel.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Supported", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("generateContent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelTestSection(
    models: List<GeminiModel>,
    selectedModel: GeminiModel?,
    onModelSelected: (GeminiModel) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    testState: ModelTestState,
    onTestClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TEST MODEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassTextField(
                    value = selectedModel?.displayName?.ifEmpty { selectedModel.name } ?: selectedModel?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.displayName.ifEmpty { model.name }) },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TEST PROMPT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            
            Button(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedModel != null && prompt.isNotBlank() && testState !is ModelTestState.Testing,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Run Model Test")
            }
        }
        
        when (testState) {
            is ModelTestState.Testing -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running model test...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is ModelTestState.Success -> {
                com.example.ui.components.glass.GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isDark = com.example.ui.theme.AppTheme.colors.isDark
                            val successColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            val successColorMuted = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Success", tint = successColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MODEL TEST PASSED", fontWeight = FontWeight.Bold, color = successColor)
                        }
                        Text("Response", style = MaterialTheme.typography.labelMedium, color = successColor)
                        Text(testState.response, style = MaterialTheme.typography.bodyMedium, color = successColorMuted)
                        
                        Text("Latency", style = MaterialTheme.typography.labelMedium, color = successColor)
                        Text("${testState.latencyMs} ms", style = MaterialTheme.typography.bodyMedium, color = successColorMuted)
                    }
                }
            }
            is ModelTestState.Error -> {
                ErrorDiagnosticsCard(testState.code, testState.message, onRetry = onTestClick)
            }
            else -> {}
        }
    }
}

@Composable
fun ErrorDiagnosticsCard(code: String, message: String, onRetry: () -> Unit) {
    val isNetwork = code.equals("Network", ignoreCase = true)
    val isRateLimit = code == "429"
    val isAuthError = code == "401" || code == "403"
    
    val isDark = com.example.ui.theme.AppTheme.colors.isDark
    val containerColor = if (isNetwork || isRateLimit) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)
    val contentColor = if (isDark) {
        if (isNetwork || isRateLimit) Color(0xFFFFB74D) else Color(0xFFE57373)
    } else {
        if (isNetwork || isRateLimit) Color(0xFFE65100) else Color(0xFFC62828)
    }
    val icon = if (isNetwork || isRateLimit) Icons.Default.Warning else Icons.Default.Error
    val title = when (code) {
        "401" -> "AUTHENTICATION FAILED"
        "403" -> "ACCESS DENIED"
        "429" -> "RATE LIMIT / QUOTA"
        "Network" -> "CONNECTION FAILED"
        else -> "ERROR"
    }
    
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, title, tint = contentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = contentColor)
            }
            if (!isNetwork) {
                Text("HTTP $code", style = MaterialTheme.typography.labelMedium, color = contentColor)
            }
            Text(message, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            
            if (isAuthError) {
                Text("Possible causes:\n• Invalid API key\n• Incorrect key\n• Key revoked", style = MaterialTheme.typography.bodySmall, color = contentColor)
            } else if (isRateLimit) {
                Text("Possible causes:\n• Requests per minute\n• Tokens per minute\n• Daily quota", style = MaterialTheme.typography.bodySmall, color = contentColor)
            } else if (isNetwork) {
                Text("Check your internet connection.", style = MaterialTheme.typography.bodySmall, color = contentColor)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetry, 
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
            ) {
                Text("Try Again")
            }
        }
    }
}
