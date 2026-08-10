package com.example.ui.screens.processing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.models.BlueprintSummary
import com.example.domain.services.ai.BlueprintService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class StepState {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class ChecklistItem(
    val title: String,
    val state: StepState
)

data class ProcessingState(
    val projectTitle: String = "Lecture Note Extraction",
    val percentage: Int = 0,
    val checklist: List<ChecklistItem> = listOf(
        ChecklistItem("Extracting raw text via OCR", StepState.PENDING),
        ChecklistItem("Building Knowledge Blueprint", StepState.PENDING),
        ChecklistItem("Synthesizing Study Notes", StepState.PENDING),
        ChecklistItem("Generating Flashcards", StepState.PENDING)
    ),
    val blueprintSummary: BlueprintSummary? = null,
    val isFinished: Boolean = false
)

class ProcessingViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private var hasStarted = false

    fun startProcessing(context: Context, projectId: Int) {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            // Step 1: OCR Processing (simulated wait since text is already extracted in NewProjectViewModel)
            updateStepState(0, StepState.IN_PROGRESS, 25)

            // Fetch project from DB
            val projectDao = com.example.data.database.AppDatabase.getDatabase(context).projectDao()
            val project = projectDao.getProjectById(projectId)
            val extractedText = project?.sourceText ?: ""

            delay(1500) // pretend we are extracting
            updateStepState(0, StepState.COMPLETED, 50)

            // Step 2: Building Knowledge Blueprint
            updateStepState(1, StepState.IN_PROGRESS, 75)

            try {
                val dataStore = AiSettingsDataStore(context)
                val settings = dataStore.aiSettingsFlow.first()

                val aiClient = AiNetworkClient(
                    provider = settings.ai1Provider.name,
                    apiKey = settings.ai1ApiKey,
                    model = settings.ai1Model.ifBlank { "gemini-2.5-flash" },
                    temperature = settings.ai2Temperature
                )

                val blueprintService = BlueprintService(aiClient)
                val result = blueprintService.generateBlueprint(extractedText)

                if (result.isSuccess) {
                    _state.update { it.copy(blueprintSummary = result.getOrNull()) }
                    updateStepState(1, StepState.COMPLETED, 100)
                    // ONLY finish and move to the next screen if successful!
                    _state.update { it.copy(isFinished = true) }
                } else {
                    // Stop spinning and show error on the UI
                    updateStepState(1, StepState.FAILED, 75)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Stop spinning and show error on the UI
                updateStepState(1, StepState.FAILED, 75)
            }
        }
    }

    private fun updateStepState(index: Int, state: StepState, percentage: Int) {
        _state.update { currentState ->
            val updatedChecklist = currentState.checklist.toMutableList().apply {
                this[index] = this[index].copy(state = state)
            }
            currentState.copy(
                checklist = updatedChecklist,
                percentage = percentage
            )
        }
    }
}
