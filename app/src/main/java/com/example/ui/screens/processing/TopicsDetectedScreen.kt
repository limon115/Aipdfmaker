package com.example.ui.screens.processing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.example.domain.models.Topic
import com.example.ui.screens.blueprint.BlueprintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsDetectedScreen(
    blueprintViewModel: BlueprintViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val blueprintState by blueprintViewModel.state.collectAsState()
    val topics = blueprintState?.topics ?: emptyList()

    var topicToDelete by remember { mutableStateOf<Pair<Int, Topic>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    var newTopicDuration by remember { mutableStateOf("20") }

    // Delete Confirmation Dialog
    if (topicToDelete != null) {
        val (targetIndex, targetTopic) = topicToDelete!!
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete Topic?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${targetTopic.title}\"? It will be excluded from the generated study notes.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        blueprintViewModel.deleteTopicAt(targetIndex)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Topic Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Add Topic", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTopicTitle,
                        onValueChange = { newTopicTitle = it },
                        label = { Text("Topic Title") },
                        placeholder = { Text("e.g. Differentiation Rules") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTopicDuration,
                        onValueChange = { newTopicDuration = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Est. Duration (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTopicTitle.trim()
                        if (trimmed.isNotBlank()) {
                            val duration = newTopicDuration.toIntOrNull() ?: 20
                            blueprintViewModel.addTopic(trimmed, duration)
                            newTopicTitle = ""
                            newTopicDuration = "20"
                            showAddDialog = false
                        }
                    },
                    enabled = newTopicTitle.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topics Detected", fontWeight = FontWeight.Bold) },
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = blueprintState != null && topics.isNotEmpty()
                ) {
                    Text(
                        if (topics.isNotEmpty()) "Continue (${topics.size} Topics)" else "Continue",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (blueprintState == null) {
                com.example.ui.components.WavyLoadingScreen(
                    message = "Detecting Lecture Topics...",
                    subMessage = "Extracting key concepts, formulas, and headings"
                )
            } else {
                com.example.ui.components.glass.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = if (topics.isNotEmpty()) {
                            "We found ${topics.size} topics in this lecture. Tap the delete icon on any topic you want to exclude."
                        } else {
                            "All topics were removed. Tap 'Add Topic' below to add topics for note generation."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(topics, key = { index, topic -> "${topic.title}_$index" }) { index, topic ->
                        TopicRow(
                            index = index + 1,
                            topic = topic,
                            onDelete = {
                                topicToDelete = Pair(index, topic)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Topic")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Topic")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TopicRow(
    index: Int,
    topic: com.example.domain.models.Topic,
    onDelete: () -> Unit
) {
    val isDark = com.example.ui.theme.AppTheme.colors.isDark
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1B5E20).copy(alpha = 0.5f) else Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Est. duration: ${topic.durationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Topic"
                )
            }
        }
    }
}
