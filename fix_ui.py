with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt', 'r') as f:
    content = f.read()
content = content.replace('''                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary) // 
                    
                    modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )''', '                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)')
with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt', 'w') as f:
    f.write(content)
