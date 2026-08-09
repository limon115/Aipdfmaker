package com.example.ui.screens.processing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.domain.models.BlueprintSummary
import com.example.domain.services.worker.NoteGenerationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.lifecycle.asFlow
import java.util.UUID

data class NoteGenerationState(
    val title: String = "Generating Study Notes",
    val checklist: List<ChecklistItem> = emptyList(),
    val isFinished: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val workId: UUID? = null
)

class NoteGenerationViewModel : ViewModel() {
    private val _state = MutableStateFlow(NoteGenerationState())
    val state: StateFlow<NoteGenerationState> = _state.asStateFlow()
    
    private var hasStarted = false

    fun resumeObservation(context: Context, projectId: Int) {
        if (hasStarted) return
        hasStarted = true
        
        val workManager = WorkManager.getInstance(context)
        
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData("NoteGen_${projectId}").asFlow().collect { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo != null) {
                    val progress = workInfo.progress.getInt(NoteGenerationWorker.PROGRESS, -1)
                    val total = workInfo.progress.getInt(NoteGenerationWorker.TOTAL, 0)
                    val currentTopic = workInfo.progress.getString(NoteGenerationWorker.CURRENT_TOPIC) ?: "Unknown Topic"
                    
                    if (progress != -1 && total > 0) {
                        _state.update { currentState ->
                            if (currentState.checklist.isEmpty() || currentState.checklist.size != total) {
                                val updatedChecklist = mutableListOf<ChecklistItem>()
                                for (i in 0 until total) {
                                    if (i < progress) {
                                        updatedChecklist.add(ChecklistItem("Topic ${i+1}", StepState.COMPLETED))
                                    } else if (i == progress) {
                                        updatedChecklist.add(ChecklistItem(currentTopic, StepState.IN_PROGRESS))
                                    } else {
                                        updatedChecklist.add(ChecklistItem("Pending Topic ${i+1}", StepState.PENDING))
                                    }
                                }
                                currentState.copy(checklist = updatedChecklist)
                            } else {
                                val mergedChecklist = currentState.checklist.toMutableList()
                                for (i in 0 until total) {
                                    if (i < progress) {
                                        mergedChecklist[i] = mergedChecklist[i].copy(state = StepState.COMPLETED)
                                    } else if (i == progress) {
                                        mergedChecklist[i] = mergedChecklist[i].copy(title = "Generating: $currentTopic", state = StepState.IN_PROGRESS)
                                    } else {
                                        mergedChecklist[i] = mergedChecklist[i].copy(state = StepState.PENDING)
                                    }
                                }
                                currentState.copy(checklist = mergedChecklist)
                            }
                        }
                    }
                    
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        _state.update { currentState ->
                            val finalChecklist = currentState.checklist.map { it.copy(state = StepState.COMPLETED) }
                            currentState.copy(checklist = finalChecklist, isFinished = true)
                        }
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        val error = workInfo.outputData.getString(NoteGenerationWorker.ERROR) ?: "Unknown Error"
                        _state.update { it.copy(hasError = true, errorMessage = error) }
                    }
                }
            }
        }
    }

    fun startGenerationLoop(context: Context, projectId: Int, blueprint: BlueprintSummary, sourceText: String) {
        if (hasStarted) return
        
        val initialChecklist = blueprint.topics.map { topic ->
            ChecklistItem("Generating: ${topic.title}", StepState.PENDING)
        }
        _state.update { it.copy(checklist = initialChecklist) }
        
        val blueprintJson = Json.encodeToString(blueprint)
        
        val inputData = Data.Builder()
            .putInt("PROJECT_ID", projectId)
            .putString("BLUEPRINT_JSON", blueprintJson)
            .build()
            
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<NoteGenerationWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
            
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "NoteGen_${projectId}",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        _state.update { it.copy(workId = workRequest.id) }
        
        // Start observing
        resumeObservation(context, projectId)
    }
}
