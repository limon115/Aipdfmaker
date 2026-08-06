package com.example.ui.screens.processing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.models.BlueprintSummary
import com.example.ui.screens.blueprint.BlueprintViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteGenerationScreen(
    projectId: Int,
    blueprintViewModel: BlueprintViewModel,
    onNavigateNext: () -> Unit
) {
    val viewModel: NoteGenerationViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val blueprintState by blueprintViewModel.state.collectAsState()
    val context = LocalContext.current
    
    var sourceText by remember { mutableStateOf("") }
    
    LaunchedEffect(projectId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val project = db.projectDao().getProjectById(projectId)
            sourceText = project?.sourceText ?: ""
        }
    }

    LaunchedEffect(blueprintState, sourceText) {
        val blueprint = blueprintState
        if (blueprint != null && sourceText.isNotEmpty()) {
            viewModel.startGenerationLoop(context, projectId, blueprint, sourceText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title, fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = state.isFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View Final Notes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            if (state.hasError) {
                Text(
                    text = state.errorMessage ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(state.checklist) { _, item ->
                    ChecklistRow(item = item)
                }
            }
        }
    }
}
