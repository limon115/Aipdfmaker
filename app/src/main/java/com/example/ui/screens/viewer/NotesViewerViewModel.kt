package com.example.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ProjectDao
import com.example.data.database.DocumentSnippetDao
import com.example.domain.repository.LatexCompilerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class NotesViewerState(
    val latexContent: String = "",
    val isLoading: Boolean = true,
    val generatedPdfFile: File? = null,
    val generatedTexFile: File? = null,
    val projectName: String = "Project",
    val outputFormat: String = "PDF",
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f
)

class NotesViewerViewModel(
    private val snippetDao: DocumentSnippetDao,
    private val compilerRepository: LatexCompilerRepository,
    private val projectDao: ProjectDao
) : ViewModel() {
    private val _state = MutableStateFlow(NotesViewerState())
    val state: StateFlow<NotesViewerState> = _state.asStateFlow()
    val exportProgress: StateFlow<Float> = MutableStateFlow(0f)

    fun loadData(projectId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val project = projectDao.getProjectById(projectId)
                
                project?.let {
                    if (it.status != "Completed") {
                        val updatedProject = it.copy(
                            status = "Completed",
                            lastUpdated = System.currentTimeMillis()
                        )
                        projectDao.updateProject(updatedProject)
                    }
                }

                val snippets = snippetDao.getSnippetsForProject(projectId).first()
                val masterLatex = snippets.joinToString("\n\n") { it.jsonContent }

                _state.update { it.copy(
                    latexContent = masterLatex,
                    isLoading = false,
                    projectName = project?.title ?: "Project",
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, latexContent = "Error loading content.") }
            }
        }
    }

    fun exportDocument(onComplete: (File?, File) -> Unit) {
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val result = compilerRepository.compileAndExportPdf(
                    projectName = _state.value.projectName,
                    latexContent = _state.value.latexContent
                )
                if (result.isSuccess) {
                    val (pdf, tex) = result.getOrThrow()
                    _state.update { it.copy(generatedPdfFile = pdf, generatedTexFile = tex, isExporting = false) }
                    onComplete(pdf, tex)
                } else {
                    val safeName = _state.value.projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
                    val tex = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "aipdfs/\$safeName/document.tex")
                    _state.update { it.copy(isExporting = false) }
                    onComplete(null, tex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isExporting = false) }
                onComplete(null, File(""))
            }
        }
    }
}
