with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "r") as f:
    text = f.read()

target_scaffold_start = """    Scaffold(
        topBar = {"""
replacement_scaffold_start = """    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {"""
if target_scaffold_start in text:
    text = text.replace(target_scaffold_start, replacement_scaffold_start)

target_scaffold_end = """        }
    }
}"""
replacement_scaffold_end = """        }
    }
    if (state.isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .androidx.compose.foundation.clickable(enabled = false) {},
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

if target_scaffold_end in text:
    text = text.replace(target_scaffold_end, replacement_scaffold_end)
else:
    print("target_scaffold_end not found")

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "w") as f:
    f.write(text)

