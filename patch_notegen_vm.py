import re

with open("app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt", "r") as f:
    text = f.read()

# Add resumeObservation method
resume_method = """
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
                            val updatedChecklist = mutableListOf<ChecklistItem>()
                            for (i in 0 until total) {
                                if (i < progress) {
                                    updatedChecklist.add(ChecklistItem(if (i == progress - 1) currentTopic else "Topic ${i+1}", StepState.COMPLETED))
                                } else if (i == progress) {
                                    updatedChecklist.add(ChecklistItem(currentTopic, StepState.IN_PROGRESS))
                                } else {
                                    updatedChecklist.add(ChecklistItem("Pending Topic ${i+1}", StepState.PENDING))
                                }
                            }
                            // To prevent replacing known titles from initialChecklist with "Topic X",
                            // we only update if currentState.checklist is empty or we merge carefully.
                            if (currentState.checklist.isEmpty() || currentState.checklist.size != total) {
                                currentState.copy(checklist = updatedChecklist)
                            } else {
                                val mergedChecklist = currentState.checklist.toMutableList()
                                for (i in 0 until total) {
                                    if (i < progress) {
                                        mergedChecklist[i] = mergedChecklist[i].copy(state = StepState.COMPLETED)
                                    } else if (i == progress) {
                                        mergedChecklist[i] = mergedChecklist[i].copy(state = StepState.IN_PROGRESS)
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
"""

# Find the place to inject it
target = r"fun startGenerationLoop\(context: Context, projectId: Int, blueprint: BlueprintSummary, sourceText: String\) \{"

text = text.replace(target, resume_method + "\n    " + target)

# Remove the old observer inside startGenerationLoop and just call resumeObservation!
# Actually it's easier to just call resumeObservation inside startGenerationLoop
# and remove the duplicate observer logic.

with open("app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt", "w") as f:
    f.write(text)
