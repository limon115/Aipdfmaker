package com.example.ui.screens.debugger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DocumentSnippetEntity
import com.example.data.database.ProjectEntity
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class LatexDebuggerState(
    val latexCode: String = "",
    val logContent: String = "",
    val isDebugging: Boolean = false,
    val error: String? = null
)

class LatexDebuggerViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LatexDebuggerState())
    val state = _state.asStateFlow()
    
    private val db = AppDatabase.getDatabase(application)
    private val dataStore = AiSettingsDataStore(application)
    private var debugJob: kotlinx.coroutines.Job? = null

    fun updateLatexCode(code: String) { _state.update { it.copy(latexCode = code) } }
    fun updateLogContent(log: String) { _state.update { it.copy(logContent = log) } }
    fun dismissError() { _state.update { it.copy(error = null) } }

    fun cancelDebugging() {
        debugJob?.cancel()
        debugJob = null
        _state.update { it.copy(isDebugging = false) }
    }

    fun loadFileContent(uri: Uri, isLatex: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: ""
                
                withContext(Dispatchers.Main) {
                    if (isLatex) {
                        updateLatexCode(content)
                    } else {
                        updateLogContent(content)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun debugLatex(onSuccess: (Int) -> Unit) {
        if (_state.value.latexCode.isBlank()) {
            _state.update { it.copy(error = "Please enter or upload LaTeX code to debug.") }
            return
        }

        _state.update { it.copy(isDebugging = true, error = null) }
        
        debugJob?.cancel()
        debugJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = dataStore.aiSettingsFlow.first()
                val candidates = settings.resolveCandidates(primarySlot = 3)

                if (candidates.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isDebugging = false, error = "No AI API key found. Please configure your API key in AI Settings.") }
                    }
                    return@launch
                }

                var debuggedLatex: String? = null
                var lastError: Exception? = null

                for (candidate in candidates) {
                    try {
                        val networkClient = AiNetworkClient(
                            provider = candidate.provider,
                            apiKey = candidate.apiKey,
                            model = candidate.model,
                            temperature = candidate.temperature,
                            featureName = "AI 3 - Debugger"
                        )
                        val result = networkClient.debugLatex(
                            latexCode = _state.value.latexCode,
                            logContent = _state.value.logContent
                        )
                        if (result.isNotBlank()) {
                            debuggedLatex = result
                            break
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                    }
                }

                if (debuggedLatex == null) {
                    throw lastError ?: Exception("Failed to generate debugged LaTeX script.")
                }
                
                val safeTitle = "Debugged_LaTeX_${System.currentTimeMillis() % 100000}"
                val project = ProjectEntity(
                    title = safeTitle,
                    course = "LaTeX Debugger",
                    chapter = "Debugged Document",
                    noteStyle = "Debugged",
                    outputFormat = "PDF",
                    status = "Completed",
                    pageCount = 1,
                    lastUpdated = System.currentTimeMillis(),
                    sourceText = _state.value.latexCode
                )
                val projectId = db.projectDao().insertProject(project).toInt()
                
                val snippetLatex = DocumentSnippetEntity(
                    projectId = projectId,
                    topicTitle = "Debugged Code",
                    jsonContent = _state.value.latexCode,
                    orderIndex = 0
                )
                db.documentSnippetDao().insertSnippet(snippetLatex)

                val snippetScript = DocumentSnippetEntity(
                    projectId = projectId,
                    topicTitle = "fix_script",
                    jsonContent = debuggedLatex,
                    orderIndex = 1
                )
                db.documentSnippetDao().insertSnippet(snippetScript)
                
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isDebugging = false, latexCode = "", logContent = "") }
                    onSuccess(projectId)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isDebugging = false, error = e.localizedMessage ?: "LaTeX Debugging failed.") }
                }
            }
        }
    }
}
