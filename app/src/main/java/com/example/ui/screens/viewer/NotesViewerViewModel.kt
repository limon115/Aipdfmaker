package com.example.ui.screens.viewer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.services.export.ExportEngine
import com.example.domain.services.html.HtmlMergeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class NotesViewerState(
    val htmlContent: String = "",
    val isLoading: Boolean = true,
    val generatedFile: File? = null
)

class NotesViewerViewModel(
    private val htmlMergeEngine: HtmlMergeEngine,
    private val exportEngine: ExportEngine
) : ViewModel() {

    private val _state = MutableStateFlow(NotesViewerState())
    val state: StateFlow<NotesViewerState> = _state.asStateFlow()

    fun loadHtml(projectId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val html = htmlMergeEngine.generateMasterHtml(projectId)
                _state.update { it.copy(htmlContent = html, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, htmlContent = "<p>Error loading content.</p>") }
            }
        }
    }

    fun exportDocument(fileName: String, format: String, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            if (format.equals("pdf", ignoreCase = true)) {
                exportEngine.generatePdfFromHtml(_state.value.htmlContent, fileName) { file ->
                    _state.update { it.copy(generatedFile = file) }
                    onComplete(file)
                }
            } else {
                val file = exportEngine.saveAsHtml(_state.value.htmlContent, fileName)
                _state.update { it.copy(generatedFile = file) }
                onComplete(file)
            }
        }
    }
}
