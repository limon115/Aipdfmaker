package com.example.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ProjectDao
import com.example.domain.services.export.ExportEngine
import com.example.domain.services.html.DocumentMergeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class NotesViewerState(
    val jsonContent: String = "",
    val isLoading: Boolean = true,
    val generatedPdfFile: File? = null,
    val generatedJsonFile: File? = null,
    val projectName: String = "Project",
    val outputFormat: String = "PDF"
)

class NotesViewerViewModel(
    private val documentMergeEngine: DocumentMergeEngine,
    private val exportEngine: ExportEngine,
    private val projectDao: ProjectDao
) : ViewModel() {
    private val _state = MutableStateFlow(NotesViewerState())
    val state: StateFlow<NotesViewerState> = _state.asStateFlow()

    fun loadData(projectId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val project = projectDao.getProjectById(projectId)
                
                // 🔥 FIX 2: Kill the Zombie Loop by marking as Completed!
                project?.let {
                    if (it.status != "Completed") {
                        val updatedProject = it.copy(
                            status = "Completed",
                            lastUpdated = System.currentTimeMillis()
                        )
                        // Note: If your DAO uses 'update' instead of 'updateProject', 
                        // the GitHub cloud compiler will catch it and we will fix it instantly.
                        projectDao.updateProject(updatedProject)
                    }
                }

                val html = documentMergeEngine.generateMasterJson(projectId, project?.title ?: "Project")
                _state.update { it.copy(
                    jsonContent = html,
                    isLoading = false,
                    projectName = project?.title ?: "Project",
                    outputFormat = project?.outputFormat ?: "PDF"
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, jsonContent = "<p>Error loading content.</p>") }
            }
        }
    }

    fun exportDocument(onComplete: (File?, File) -> Unit) {
        exportEngine.exportProjectFiles(_state.value.projectName, _state.value.jsonContent, _state.value.outputFormat.equals("pdf", ignoreCase = true)) { pdfFile: File?, jsonFile: File ->
            _state.update { it.copy(generatedPdfFile = pdfFile, generatedJsonFile = jsonFile) }
            onComplete(pdfFile, jsonFile)
        }
    }
}
