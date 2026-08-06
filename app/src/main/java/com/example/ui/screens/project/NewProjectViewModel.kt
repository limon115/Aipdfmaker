package com.example.ui.screens.project

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.services.ocr.LocalOcrEngine
import com.example.domain.services.pdf.PdfRendererService
import com.example.data.database.ProjectDao
import com.example.data.database.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewProjectState(
    val youtubeUrl: String = "",
    val includeCaptions: Boolean = true,
    val detectChapters: Boolean = true,
    val language: String = "Auto",
    val pdfUri: String? = null,
    val pdfFileName: String? = null,
    val isProcessingPdf: Boolean = false,
    val extractedText: String = "",
    val projectTitle: String = "",
    val course: String = "",
    val chapter: String = "",
    val description: String = "",
    val noteStyle: String = "Study Notes (Detailed)",
    val outputFormat: String = "PDF"
)

class NewProjectViewModel(private val projectDao: ProjectDao) : ViewModel() {
    private val _state = MutableStateFlow(NewProjectState())
    val state: StateFlow<NewProjectState> = _state.asStateFlow()

    fun createProject(onProjectCreated: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            val project = ProjectEntity(
                title = currentState.projectTitle.ifBlank { "Untitled Project" },
                course = currentState.course,
                chapter = currentState.chapter,
                noteStyle = currentState.noteStyle,
                outputFormat = currentState.outputFormat,
                status = "Processing",
                pageCount = 0,
                lastUpdated = System.currentTimeMillis()
            )
            val id = projectDao.insertProject(project).toInt()
            onProjectCreated(id)
        }
    }

    fun processPdf(uri: Uri, context: Context, fileName: String?) {
        updatePdfUri(uri.toString(), fileName)
        _state.update { it.copy(isProcessingPdf = true) }
        viewModelScope.launch {
            try {
                val pdfRenderer = PdfRendererService(context)
                val bitmaps = pdfRenderer.renderPdfToBitmaps(uri)
                val ocrEngine = LocalOcrEngine()
                val text = ocrEngine.extractTextFromBitmaps(bitmaps)
                _state.update { it.copy(extractedText = text) }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _state.update { it.copy(isProcessingPdf = false) }
            }
        }
    }

    fun updatePdfUri(uri: String?, fileName: String?) {
        _state.update { it.copy(pdfUri = uri, pdfFileName = fileName) }
    }

    fun updateYoutubeUrl(url: String) {
        _state.update { it.copy(youtubeUrl = url) }
    }

    fun updateIncludeCaptions(include: Boolean) {
        _state.update { it.copy(includeCaptions = include) }
    }

    fun updateDetectChapters(detect: Boolean) {
        _state.update { it.copy(detectChapters = detect) }
    }

    fun updateLanguage(lang: String) {
        _state.update { it.copy(language = lang) }
    }

    fun updateProjectTitle(title: String) {
        _state.update { it.copy(projectTitle = title) }
    }

    fun updateCourse(course: String) {
        _state.update { it.copy(course = course) }
    }

    fun updateChapter(chapter: String) {
        _state.update { it.copy(chapter = chapter) }
    }

    fun updateDescription(desc: String) {
        _state.update { it.copy(description = desc) }
    }

    fun updateNoteStyle(style: String) {
        _state.update { it.copy(noteStyle = style) }
    }

    fun updateOutputFormat(format: String) {
        _state.update { it.copy(outputFormat = format) }
    }
}
