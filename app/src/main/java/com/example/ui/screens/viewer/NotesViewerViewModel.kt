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
    val fixScript: String? = null,
    val isLoading: Boolean = true,
    val generatedPdfFile: File? = null,
    val generatedTexFile: File? = null,
    val project: com.example.data.database.ProjectEntity? = null,
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
                    project = project,
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, latexContent = "Error loading content.") }
            }
        }
    }

    fun exportDocument(onComplete: (File?, File) -> Unit, onError: (String) -> Unit) {
        _state.update { it.copy(isExporting = true, exportProgress = 0f) }
        viewModelScope.launch {
            try {
                val result = compilerRepository.compileAndExportPdf(
                    onProgress = { progress ->
                        _state.update { it.copy(exportProgress = progress) }
                    },
                    project = _state.value.project!!,
                    latexContent = _state.value.latexContent,
                    fixScript = _state.value.fixScript
                )
                if (result.isSuccess) {
                    val (pdf, tex) = result.getOrThrow()
                    _state.update { it.copy(generatedPdfFile = pdf, generatedTexFile = tex, isExporting = false) }
                    onComplete(pdf, tex)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _state.update { it.copy(isExporting = false) }
                    onError(error)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isExporting = false) }
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
