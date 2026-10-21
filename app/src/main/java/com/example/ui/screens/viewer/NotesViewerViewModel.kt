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
    val exportProgress: Float = 0f,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val lastSaveTimestamp: Long = 0L
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
                val fixScriptSnippet = snippets.find { it.topicTitle == "fix_script" }
                val latexSnippets = snippets.filter { it.topicTitle != "fix_script" }
                val masterLatex = latexSnippets.joinToString("\n\n") { it.jsonContent }

                val existingPdf = project?.let { compilerRepository.getCompiledPdf(it) }

                _state.update { it.copy(
                    latexContent = masterLatex,
                    fixScript = fixScriptSnippet?.jsonContent,
                    isLoading = false,
                    project = project,
                    generatedPdfFile = existingPdf,
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, latexContent = "Error loading content.") }
            }
        }
    }

    fun updateLatexContent(newContent: String) {
        _state.update { it.copy(latexContent = newContent, hasUnsavedChanges = true) }
    }

    fun saveLatexContent(newContent: String? = null, onComplete: (() -> Unit)? = null) {
        val contentToSave = newContent ?: _state.value.latexContent
        _state.update { it.copy(latexContent = contentToSave, isSaving = true) }
        viewModelScope.launch {
            try {
                val currentProject = _state.value.project
                val projectId = currentProject?.id
                if (projectId != null) {
                    val snippets = snippetDao.getSnippetsForProject(projectId).first()
                    val latexSnippet = snippets.find { it.topicTitle != "fix_script" }
                    if (latexSnippet != null) {
                        snippetDao.insertSnippet(latexSnippet.copy(jsonContent = contentToSave))
                    } else {
                        snippetDao.insertSnippet(
                            com.example.data.database.DocumentSnippetEntity(
                                projectId = projectId,
                                topicTitle = "LaTeX Source",
                                jsonContent = contentToSave,
                                orderIndex = 0
                            )
                        )
                    }
                    try {
                        compilerRepository.writeTexFile(currentProject, contentToSave)
                    } catch (e: Exception) {
                        timber.log.Timber.w(e, "Failed to write main.tex during save")
                    }
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        lastSaveTimestamp = System.currentTimeMillis()
                    )
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isSaving = false) }
                onComplete?.invoke()
            }
        }
    }

    fun exportDocument(onComplete: (File?, File) -> Unit, onError: (String) -> Unit) {
        val currentProject = _state.value.project
        if (currentProject == null) {
            onError("Project details are still loading. Please wait a moment and try again.")
            return
        }
        _state.update { it.copy(isExporting = true, exportProgress = 0f) }
        viewModelScope.launch {
            try {
                // Ensure unsaved changes are saved before compilation
                if (_state.value.hasUnsavedChanges) {
                    val projectId = currentProject.id
                    val snippets = snippetDao.getSnippetsForProject(projectId).first()
                    val latexSnippet = snippets.find { it.topicTitle != "fix_script" }
                    if (latexSnippet != null) {
                        snippetDao.insertSnippet(latexSnippet.copy(jsonContent = _state.value.latexContent))
                    }
                    try {
                        compilerRepository.writeTexFile(currentProject, _state.value.latexContent)
                    } catch (_: Exception) {}
                    _state.update { it.copy(hasUnsavedChanges = false) }
                }

                val result = compilerRepository.compileAndExportPdf(
                    onProgress = { progress ->
                        _state.update { it.copy(exportProgress = progress) }
                    },
                    project = currentProject,
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
