import re
with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val latexContent: String = "",\n    val isLoading: Boolean = true',
    'val latexContent: String = "",\n    val fixScript: String? = null,\n    val isLoading: Boolean = true'
)

loadData_old = '''                val snippets = snippetDao.getSnippetsForProject(projectId).first()
                val masterLatex = snippets.joinToString("\\n\\n") { it.jsonContent }
                _state.update { it.copy(
                    latexContent = masterLatex,
                    isLoading = false,
                    projectName = project?.title ?: "Project",
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }'''

loadData_new = '''                val snippets = snippetDao.getSnippetsForProject(projectId).first()
                val masterLatex = snippets.joinToString("\\n\\n") { it.jsonContent }
                
                val isDebugged = project?.noteStyle == "Debugged"
                val finalLatexContent = if (isDebugged) project.sourceText else masterLatex
                val fixScript = if (isDebugged) masterLatex else null

                _state.update { it.copy(
                    latexContent = finalLatexContent,
                    fixScript = fixScript,
                    isLoading = false,
                    projectName = project?.title ?: "Project",
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }'''

content = content.replace(loadData_old, loadData_new)

content = content.replace(
    'val result = compilerRepository.compileAndExportPdf(\n                    projectName = _state.value.projectName,\n                    latexContent = _state.value.latexContent\n                )',
    'val result = compilerRepository.compileAndExportPdf(\n                    projectName = _state.value.projectName,\n                    latexContent = _state.value.latexContent,\n                    fixScript = _state.value.fixScript\n                )'
)

with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'w') as f:
    f.write(content)
