package com.example.ui.screens.blueprint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintSummaryScreen(
    viewModel: BlueprintViewModel = viewModel(),
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blueprint Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
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
                    )
                ) {
                    Text("Start Generating Notes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { innerPadding ->
        if (state == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val safeState = state!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Top Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Course: ${safeState.courseName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chapter: ${safeState.chapterName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Middle Section (Cards)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        MetricCard(
                            title = "Topics",
                            subtitle = "${safeState.topics.size} topics found",
                            icon = Icons.AutoMirrored.Filled.List,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Key Formulas",
                            subtitle = "${safeState.formulaCount} formulas found",
                            icon = Icons.Default.Functions,
                            iconTint = Color(0xFFE91E63), // Pink
                            iconBg = Color(0xFFE91E63).copy(alpha = 0.1f)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Definitions",
                            subtitle = "${safeState.definitionCount} definitions found",
                            icon = Icons.Default.Book,
                            iconTint = Color(0xFF4CAF50), // Green
                            iconBg = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Examples",
                            subtitle = "${safeState.exampleCount} examples found",
                            icon = Icons.Default.Description,
                            iconTint = Color(0xFFFF9800), // Orange
                            iconBg = Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Diagrams & Tables",
                            subtitle = "${safeState.diagramCount} diagrams found",
                            icon = Icons.Default.Image,
                            iconTint = Color(0xFF2196F3), // Blue
                            iconBg = Color(0xFF2196F3).copy(alpha = 0.1f)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Exam Tips",
                            subtitle = "${safeState.examTipCount} exam tips found",
                            icon = Icons.Default.Lightbulb,
                            iconTint = Color(0xFF9C27B0), // Purple
                            iconBg = Color(0xFF9C27B0).copy(alpha = 0.1f)
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp)) // padding for bottom bar
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
