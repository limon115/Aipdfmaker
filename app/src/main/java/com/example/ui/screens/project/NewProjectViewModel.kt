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

    
    fun loadProject(projectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = projectDao.getProjectById(projectId)
            if (project != null) {
                _state.update {
                    it.copy(
                        projectTitle = project.title,
                        course = project.course,
                        chapter = project.chapter,
                        noteStyle = project.noteStyle,
                        outputFormat = project.outputFormat,
                        extractedText = project.sourceText
                    )
                }
            }
        }
    }

    fun createProject(onProjectCreated: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            val project = ProjectEntity(
                title = currentState.projectTitle.ifBlank { "Untitled Project" },
                course = currentState.course,
                chapter = currentState.chapter, description = currentState.description,
                noteStyle = currentState.noteStyle,
                outputFormat = currentState.outputFormat,
                status = "Processing",
                pageCount = 0,
                lastUpdated = System.currentTimeMillis(),
                sourceText = currentState.extractedText
            )
            val id = projectDao.insertProject(project).toInt()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onProjectCreated(id)
            }
        }
    }

    fun processFile(uri: Uri, context: Context, fileName: String?) {
        val resolvedName = fileName ?: com.example.utils.FileUtils.getDisplayNameFromUri(context, uri)
        updatePdfUri(uri.toString(), resolvedName)
        _state.update { it.copy(isProcessingPdf = true) }
        viewModelScope.launch {
            try {
                val category = com.example.utils.FileUtils.getFileType(context, uri, resolvedName)
                val text = when (category) {
                    com.example.utils.FileUtils.FileCategory.PDF -> {
                        val pdfRenderer = PdfRendererService(context)
                        val ocrEngine = LocalOcrEngine()
                        pdfRenderer.extractTextFromPdf(uri, ocrEngine)
                    }
                    com.example.utils.FileUtils.FileCategory.TEXT -> {
                        val txtReader = com.example.domain.services.file.TxtFileReaderService()
                        txtReader.readTextFromUri(uri, context)
                    }
                    com.example.utils.FileUtils.FileCategory.IMAGE -> {
                        val ocrEngine = LocalOcrEngine()
                        ocrEngine.extractTextFromImageUri(uri, context)
                    }
                    com.example.utils.FileUtils.FileCategory.UNKNOWN -> {
                        try {
                            val txtReader = com.example.domain.services.file.TxtFileReaderService()
                            txtReader.readTextFromUri(uri, context)
                        } catch (e: Exception) {
                            val ocrEngine = LocalOcrEngine()
                            ocrEngine.extractTextFromImageUri(uri, context)
                        }
                    }
                }
                _state.update { it.copy(extractedText = text) }
            } catch (e: Exception) {
                com.example.utils.AppLogger.e("NewProjectViewModel", "Error processing file $resolvedName", e)
                _state.update { it.copy(extractedText = "Error processing file: ${e.message}") }
            } finally {
                _state.update { it.copy(isProcessingPdf = false) }
            }
        }
    }

    fun processPdf(uri: Uri, context: Context, fileName: String?) {
        processFile(uri, context, fileName)
    }

    fun updatePdfUri(uri: String?, fileName: String?) {
        _state.update { it.copy(pdfUri = uri, pdfFileName = fileName) }
    }

    fun updateExtractedText(text: String) {
        _state.update { it.copy(extractedText = text) }
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
