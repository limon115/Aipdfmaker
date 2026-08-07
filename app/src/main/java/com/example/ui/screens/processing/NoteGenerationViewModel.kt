package com.example.ui.screens.processing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.HtmlSnippetEntity
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.models.BlueprintSummary
import com.example.domain.services.ai.NoteGenerationService
import com.example.data.network.AiNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class NoteGenerationState(
    val title: String = "Generating Study Notes",
    val checklist: List<ChecklistItem> = emptyList(),
    val isFinished: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

class NoteGenerationViewModel : ViewModel() {
    private val _state = MutableStateFlow(NoteGenerationState())
    val state: StateFlow<NoteGenerationState> = _state.asStateFlow()

    private var hasStarted = false

    fun startGenerationLoop(context: Context, projectId: Int, blueprint: BlueprintSummary, sourceText: String) {
        if (hasStarted) return
        hasStarted = true

        val initialChecklist = blueprint.topics.map { topic ->
            ChecklistItem("Generating: ${topic.title}", StepState.PENDING)
        }
        _state.update { it.copy(checklist = initialChecklist) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = AiSettingsDataStore(context)
                val settings = dataStore.aiSettingsFlow.first()
                
                // Just use a dummy client for the service dependency since the service creates its own for generation
                val dummyClient = AiNetworkClient(
                    provider = settings.ai2Provider.name,
                    apiKey = settings.ai2ApiKey,
                    model = settings.ai2Model,
                    temperature = settings.ai2Temperature
                )
                val service = NoteGenerationService(dummyClient)
                
                val db = AppDatabase.getDatabase(context)
                val snippetDao = db.htmlSnippetDao()
                
                val blueprintContext = Json.encodeToString(blueprint)

                blueprint.topics.forEachIndexed { index, topic ->
                    updateStepState(index, StepState.IN_PROGRESS)
                    
                    try {
                        val html = service.generateHtmlForTopic(
                            topicTitle = topic.title,
                            blueprintContext = blueprintContext,
                            sourceText = sourceText,
                            ai2Provider = settings.ai2Provider.name,
                            ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                            ai2ApiKey = settings.ai2ApiKey,
                            ai2Temperature = settings.ai2Temperature
                        )
                        
                        val snippet = HtmlSnippetEntity(
                            projectId = projectId,
                            topicTitle = topic.title,
                            htmlContent = html,
                            orderIndex = index
                        )
                        snippetDao.insertSnippet(snippet)
                        
                        updateStepState(index, StepState.COMPLETED)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        updateStepState(index, StepState.FAILED)
                        _state.update { it.copy(hasError = true, errorMessage = "Failed to generate: ${topic.title}. You may proceed with missing notes, or retry later.") }
                    }
                }
                
                _state.update { it.copy(isFinished = true) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(hasError = true, errorMessage = e.message ?: "An error occurred during generation") }
            }
        }
    }

    private fun updateStepState(index: Int, stepState: StepState) {
        _state.update { currentState ->
            val updatedChecklist = currentState.checklist.toMutableList()
            if (index in updatedChecklist.indices) {
                updatedChecklist[index] = updatedChecklist[index].copy(state = stepState)
            }
            currentState.copy(checklist = updatedChecklist)
        }
    }
}
