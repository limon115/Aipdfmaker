import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("fun exportDocument(onComplete: (File, File) -> Unit)", "fun exportDocument(onComplete: (File?, File) -> Unit)")
content = content.replace("exportEngine.exportProjectFiles(_state.value.projectName, _state.value.htmlContent) { pdfFile, htmlFile ->", "exportEngine.exportProjectFiles(_state.value.projectName, _state.value.htmlContent) { pdfFile: File?, htmlFile: File ->")

with open('/app/applet/app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'w') as f:
    f.write(content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt', 'r') as f:
    content = f.read()

# If pdfFile is null, the user used PrintManager. We should only try to open if there's an actual file (like HTML if they selected HTML, but we always Print now)
# Wait, if we used PrintManager, the system already handles the "Save as PDF" intent. So we shouldn't show the "Open Document" intent.
# We can just check if pdfFile == null and outputFormat == pdf, then we just return.

old_intent = """                val isPdf = state.outputFormat.equals("pdf", ignoreCase = true)
                val selectedFile = if (isPdf && pdfFile != null) pdfFile else htmlFile
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    selectedFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val mimeType = if (isPdf && pdfFile != null) "application/pdf" else "text/html"
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open Document..."))"""

new_intent = """                val isPdf = state.outputFormat.equals("pdf", ignoreCase = true)
                if (isPdf && pdfFile == null) {
                    // Handled by PrintManager
                    return@exportDocument
                }
                val selectedFile = if (isPdf && pdfFile != null) pdfFile else htmlFile
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    selectedFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val mimeType = if (isPdf && pdfFile != null) "application/pdf" else "text/html"
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open Document..."))"""

content = content.replace(old_intent, new_intent)

with open('/app/applet/app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt', 'w') as f:
    f.write(content)

print("Fixed Viewer")
