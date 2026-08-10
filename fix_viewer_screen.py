import re

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "r") as f:
    text = f.read()

# First, let's remove the bad block at the very end
bad_block = """    if (state.isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {},
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Generating PDF...", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    }
}"""

if text.endswith(bad_block):
    text = text[:-len(bad_block)]
    text += "        }\n    }\n}\n"

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "w") as f:
    f.write(text)

