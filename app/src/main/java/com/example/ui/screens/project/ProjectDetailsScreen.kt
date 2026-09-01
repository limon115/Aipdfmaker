package com.example.ui.screens.project

import com.example.ui.components.glass.GlassTextField


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.os.Environment
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    viewModel: NewProjectViewModel,
    projectId: Int? = null,
    onCreateProject: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProgress: (Int) -> Unit = {},
    onNavigateToViewer: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var documentExists by remember { mutableStateOf(false) }
    var documentContent by remember { mutableStateOf("") }
    
    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.loadProject(projectId)
        }
    }
    
    LaunchedEffect(state.projectTitle, projectId) {
        if (projectId != null && state.projectTitle.isNotEmpty()) {
            val safeName = state.projectTitle.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val baseDir = File(documentsDir, "aipdfs/$safeName")
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val fallbackBaseDir = File(fallbackDir ?: context.filesDir, "aipdfs/$safeName")
            
            val jsonFile = File(baseDir, "document.json")
            val fallbackJsonFile = File(fallbackBaseDir, "document.json")
            
            if (jsonFile.exists()) {
                documentExists = true
                documentContent = jsonFile.readText()
            } else if (fallbackJsonFile.exists()) {
                documentExists = true
                documentContent = fallbackJsonFile.readText()
            } else {
                documentExists = false
                documentContent = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Details", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassTextField(
                value = state.projectTitle,
                onValueChange = viewModel::updateProjectTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            GlassTextField(
                value = state.course,
                onValueChange = viewModel::updateCourse,
                label = { Text("Course / Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            GlassTextField(
                value = state.chapter,
                onValueChange = viewModel::updateChapter,
                label = { Text("Chapter (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            GlassTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Details (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )

            Column {
                Text(
                    text = "Note Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                com.example.ui.components.glass.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Show dropdown */ },
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.noteStyle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Style")
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Output Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("PDF", "HTML")
                    formats.forEach { format ->
                        FilterChip(
                            selected = state.outputFormat == format,
                            onClick = { viewModel.updateOutputFormat(format) },
                            label = { Text(format) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (projectId != null) {
                if (documentExists) {
                    com.example.ui.components.glass.GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Content Available", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(documentContent.take(200) + if(documentContent.length > 200) "..." else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Button(
                        onClick = { onNavigateToViewer(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View Content", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = { onNavigateToProgress(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("View Progress", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.createProject { id ->
                            onCreateProject(id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Project", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
