import re

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "r") as f:
    text = f.read()

# Add exportProgress state observation
text = text.replace('val state by viewModel.state.collectAsState()', 'val state by viewModel.state.collectAsState()\n    val exportProgress by viewModel.exportProgress.collectAsState()')

# We need to insert the loading overlay at the end of the Scaffold block
# Scaffold is inside a Box if we look at previous code.
# Let's just find the end of the Scaffold block.

target = """            }
        }
    }
}
@Composable
fun ExportPreviewModal"""

replacement = """            }
        }
    }
    
    if (state.isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(32.dp)
            ) {
                Text("Generating PDF...", color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { exportProgress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(exportProgress * 100).toInt()}%", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable
fun ExportPreviewModal"""

if target in text:
    text = text.replace(target, replacement)
else:
    print("Target not found")
    
with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "w") as f:
    f.write(text)

