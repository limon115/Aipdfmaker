import os

content = """package com.example.ui.screens.processing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.domain.models.BlueprintSummary
import com.example.domain.services.worker.BlueprintWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import timber.log.Timber

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
    val isFinished: Boolean = false,
    val workState: WorkInfo.State? = null
)

class ProcessingViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private var hasStarted = false
    private val jsonFormat = Json { ignoreUnknownKeys = true; isLenient = true }

    fun startProcessing(context: Context, projectId: Int) {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            com.example.utils.AppLogger.i("ProcessingVM", "Starting processing for project $projectId")

            // Step 1: OCR Processing
            updateStepState(0, StepState.IN_PROGRESS, 25)
            delay(1500)
            updateStepState(0, StepState.COMPLETED, 50)

            // Step 2: Building Knowledge Blueprint (WorkManager)
            updateStepState(1, StepState.IN_PROGRESS, 50)
            
            val workManager = WorkManager.getInstance(context)
            val workName = "BlueprintGen_$projectId"
            
            val inputData = Data.Builder()
                .putInt("project_id", projectId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BlueprintWorker>()
                .setInputData(inputData)
                .build()

            workManager.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            launch(Dispatchers.Main) {
                workManager.getWorkInfoByIdLiveData(workRequest.id).observeForever { workInfo ->
                    if (workInfo != null) {
                        _state.update { it.copy(workState = workInfo.state) }
                        
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                val summaryJson = workInfo.outputData.getString("blueprint_summary")
                                if (!summaryJson.isNullOrEmpty()) {
                                    try {
                                        val summary = jsonFormat.decodeFromString<BlueprintSummary>(summaryJson)
                                        _state.update { it.copy(blueprintSummary = summary) }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to parse BlueprintSummary")
                                    }
                                }
                                updateStepState(1, StepState.COMPLETED, 100)
                                _state.update { it.copy(isFinished = true) }
                            }
                            WorkInfo.State.FAILED -> {
                                updateStepState(1, StepState.FAILED, 75)
                            }
                            else -> {
                                // Running or enqueued
                            }
                        }
                    }
                }
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
"""
with open("app/src/main/java/com/example/ui/screens/processing/ProcessingViewModel.kt", "w") as f:
    f.write(content)
