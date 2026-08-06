package com.example.ui.screens.settings

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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToProviderSelection: (isAi1: Boolean) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AiConfigCard(
                title = "AI #1 - Blueprint Builder",
                provider = settings.ai1Provider,
                model = settings.ai1Model,
                apiKey = settings.ai1ApiKey,
                onProviderClick = { onNavigateToProviderSelection(true) },
                onModelChange = viewModel::updateAi1Model,
                onApiKeyChange = viewModel::updateAi1ApiKey,
                onTestConnection = {
                    viewModel.testConnection(
                        isAi1 = true,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = { android.widget.Toast.makeText(context, "Connection Failed: $it", android.widget.Toast.LENGTH_LONG).show() }
                    )
                }
            )

            AiConfigCard(
                title = "AI #2 - Note Generator",
                provider = settings.ai2Provider,
                model = settings.ai2Model,
                apiKey = settings.ai2ApiKey,
                onProviderClick = { onNavigateToProviderSelection(false) },
                onModelChange = viewModel::updateAi2Model,
                onApiKeyChange = viewModel::updateAi2ApiKey,
                onTestConnection = {
                    viewModel.testConnection(
                        isAi1 = false,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = { android.widget.Toast.makeText(context, "Connection Failed: $it", android.widget.Toast.LENGTH_LONG).show() }
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
    onTestConnection: () -> Unit,
    advancedSettings: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val recommendedModels = when (provider) {
        AiProvider.GOOGLE_GEMINI -> listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro")
        AiProvider.OPENAI -> listOf("gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo")
        AiProvider.OLLAMA -> listOf("llama3", "mistral", "gemma")
        else -> listOf("gemini-1.5-pro", "gemini-1.5-flash")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Provider Selection
            Column {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onProviderClick),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = onModelChange,
                    label = { Text("Model (e.g. ${recommendedModels.first()})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
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
                    onTestConnection()
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

            // Reset isTesting after test logic resolves (since we passed it down, we can reset it inside the callback or using a side effect)
            // But we can keep it simple: the caller will show a toast. We can reset isTesting after a delay.
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

                OutlinedTextField(
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
