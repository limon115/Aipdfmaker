package com.example.ui.screens.debugger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexDebuggerScreen(
    viewModel: LatexDebuggerViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val texLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.loadFileContent(uri, isLatex = true)
        }
    }

    val logLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.loadFileContent(uri, isLatex = false)
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Error") },
            text = { Text(state.error!!) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LaTeX Debugger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Paste your LaTeX code and compiler log below, or upload the files directly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LaTeX Code (.tex)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { texLauncher.launch("*/*") }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload .tex")
                            Spacer(Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassTextField(
                        value = state.latexCode,
                        onValueChange = viewModel::updateLatexCode,
                        placeholder = { Text("Paste LaTeX code here...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                        singleLine = false
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compiler Log (main.log)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { logLauncher.launch("*/*") }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload .log")
                            Spacer(Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassTextField(
                        value = state.logContent,
                        onValueChange = viewModel::updateLogContent,
                        placeholder = { Text("Paste compiler log here...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                        singleLine = false
                    )
                }
            }

            Button(
                onClick = { viewModel.debugLatex(onSuccess = onNavigateToViewer) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                enabled = !state.isDebugging && state.latexCode.isNotBlank() && state.logContent.isNotBlank()
            ) {
                if (state.isDebugging) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.BugReport, contentDescription = "Debug")
                    Spacer(Modifier.width(8.dp))
                    Text("Debug & Rewrite Code")
                }
            }
        }
    }
}
