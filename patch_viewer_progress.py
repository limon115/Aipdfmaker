with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt", "r") as f:
    text = f.read()

target1 = """    val projectName: String = "Project",
    val outputFormat: String = "PDF"
)"""
replacement1 = """    val projectName: String = "Project",
    val outputFormat: String = "PDF",
    val isExporting: Boolean = false
)"""
if target1 in text:
    text = text.replace(target1, replacement1)

target2 = """    fun exportDocument(onComplete: (File?, File) -> Unit) {
        exportEngine.exportProjectFiles(_state.value.projectName, _state.value.jsonContent, _state.value.outputFormat.equals("pdf", ignoreCase = true)) { pdfFile: File?, jsonFile: File ->
            _state.update { it.copy(generatedPdfFile = pdfFile, generatedJsonFile = jsonFile) }
            onComplete(pdfFile, jsonFile)
        }
    }"""
replacement2 = """    fun exportDocument(onComplete: (File?, File) -> Unit) {
        _state.update { it.copy(isExporting = true) }
        exportEngine.exportProjectFiles(_state.value.projectName, _state.value.jsonContent, _state.value.outputFormat.equals("pdf", ignoreCase = true)) { pdfFile: File?, jsonFile: File ->
            _state.update { it.copy(generatedPdfFile = pdfFile, generatedJsonFile = jsonFile, isExporting = false) }
            onComplete(pdfFile, jsonFile)
        }
    }"""
if target2 in text:
    text = text.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt", "w") as f:
    f.write(text)

