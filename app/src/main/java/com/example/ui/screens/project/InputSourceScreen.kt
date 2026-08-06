package com.example.ui.screens.project

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.services.file.TxtFileReaderService
import com.example.domain.services.ocr.LocalOcrEngine
import kotlinx.coroutines.launch

data class InputSourceItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSourceScreen(
    viewModel: NewProjectViewModel,
    onNavigate: (String) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val txtReader = remember { TxtFileReaderService() }
    val ocrEngine = remember { LocalOcrEngine() }

    val txtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val text = txtReader.readTextFromUri(uri, context)
                    viewModel.updateExtractedText(text)
                    isLoading = false
                    onNavigateNext()
                } catch (e: Exception) {
                    e.printStackTrace()
                    isLoading = false
                    Toast.makeText(context, "Failed to read text file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                Toast.makeText(context, "Extracting text from image...", Toast.LENGTH_SHORT).show()
                try {
                    val text = ocrEngine.extractTextFromImageUri(uri, context)
                    viewModel.updateExtractedText(text)
                    isLoading = false
                    onNavigateNext()
                } catch (e: Exception) {
                    e.printStackTrace()
                    isLoading = false
                    Toast.makeText(context, "Failed to extract text from image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val sources = listOf(
        InputSourceItem(
            title = "YouTube Link",
            description = "Extract transcript from YouTube video",
            icon = Icons.Default.OndemandVideo,
            route = "youtube_link"
        ),
        InputSourceItem(
            title = "Upload PDF or Image",
            description = "OCR and extract content from scanned slides or PDF",
            icon = Icons.Default.PictureAsPdf,
            route = "image_input"
        ),
        InputSourceItem(
            title = "Paste Transcript",
            description = "Paste text directly from clipboard",
            icon = Icons.AutoMirrored.Filled.NoteAdd,
            route = "paste_input"
        ),
        InputSourceItem(
            title = "Import from File",
            description = "Supports TXT formats",
            icon = Icons.Default.FileUpload,
            route = "txt_input"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Project", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Choose Input Source",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sources) { source ->
                    SourceCard(
                        item = source,
                        onClick = { 
                            if (isLoading) return@SourceCard
                            when (source.route) {
                                "txt_input" -> txtPickerLauncher.launch("text/plain")
                                "image_input" -> imagePickerLauncher.launch("image/*")
                                else -> onNavigate(source.route)
                            }
                        }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "We process everything locally first for better accuracy and privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SourceCard(item: InputSourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
