package com.example.ui.screens.math

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.domain.services.worker.MathSolverWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import timber.log.Timber

sealed class MathSolverState {
    object Idle : MathSolverState()
    data class Processing(val progress: Float = 0.25f, val statusText: String = "AI is analyzing problem & generating solution...") : MathSolverState()
    data class CompilingPdf(val progress: Float = 0.75f, val statusText: String = "Compiling LaTeX solution to PDF via XeLaTeX...") : MathSolverState()
    data class EnqueuedInBackground(val progress: Float = 0.50f, val statusText: String = "Math Solver job running in background...") : MathSolverState()
    data class Success(val pdfFile: File) : MathSolverState()
    data class Error(val message: String) : MathSolverState()
}

class MathSolverViewModel : ViewModel() {
    private val _state = MutableStateFlow<MathSolverState>(MathSolverState.Idle)
    val state: StateFlow<MathSolverState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var isBackgroundMode = false
    private var currentProblemText: String = ""

    fun solveProblem(context: Context, problemText: String) {
        if (problemText.isBlank()) return
        isBackgroundMode = false
        currentProblemText = problemText
        startOrObserveWorker(context, problemText)
    }

    fun solveProblemInBackground(context: Context, problemText: String) {
        if (problemText.isBlank()) return

        val currentState = _state.value
        // If the task is already running (e.g. user clicked "Run in background" while solving),
        // seamlessly continue without cancelling or restarting. Finish that task!
        if (currentState is MathSolverState.Processing || currentState is MathSolverState.CompilingPdf) {
            isBackgroundMode = true
            val currentProgress = when (currentState) {
                is MathSolverState.Processing -> currentState.progress
                is MathSolverState.CompilingPdf -> currentState.progress
                else -> 0.50f
            }
            val currentStatus = when (currentState) {
                is MathSolverState.Processing -> currentState.statusText
                is MathSolverState.CompilingPdf -> currentState.statusText
                else -> "Math Solver job running in background..."
            }
            _state.value = MathSolverState.EnqueuedInBackground(currentProgress, currentStatus)
            return
        }

        isBackgroundMode = true
        currentProblemText = problemText
        startOrObserveWorker(context, problemText)
    }

    private fun startOrObserveWorker(context: Context, problemText: String) {
        observeJob?.cancel()
        val workManager = WorkManager.getInstance(context)
        val workName = "MathSolver_Active"

        val inputData = Data.Builder()
            .putString(MathSolverWorker.KEY_PROBLEM_TEXT, problemText)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MathSolverWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("MathSolverTag")
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        if (isBackgroundMode) {
            _state.value = MathSolverState.EnqueuedInBackground(0.20f, "Enqueuing background solver task...")
        } else {
            _state.value = MathSolverState.Processing(0.25f, "AI is analyzing problem & generating solution...")
        }

        observeJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collect { workInfo ->
                    if (workInfo != null) {
                        val progress = workInfo.progress.getFloat("PROGRESS", 0.25f)
                        val status = workInfo.progress.getString("STATUS") ?: "AI is analyzing problem & generating solution..."

                        when (workInfo.state) {
                            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                                if (isBackgroundMode) {
                                    _state.value = MathSolverState.EnqueuedInBackground(progress, status)
                                } else {
                                    if (progress >= 0.70f) {
                                        _state.value = MathSolverState.CompilingPdf(progress, status)
                                    } else {
                                        _state.value = MathSolverState.Processing(progress, status)
                                    }
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                val pdfPath = workInfo.outputData.getString(MathSolverWorker.KEY_PDF_PATH)
                                if (pdfPath != null) {
                                    val file = File(pdfPath)
                                    if (file.exists()) {
                                        _state.value = MathSolverState.Success(file)
                                    } else {
                                        _state.value = MathSolverState.Error("PDF was generated but not found.")
                                    }
                                } else {
                                    _state.value = MathSolverState.Error("Task completed, but output PDF path was missing.")
                                }
                            }
                            WorkInfo.State.FAILED -> {
                                val error = workInfo.outputData.getString(MathSolverWorker.KEY_ERROR) ?: "Task failed due to a network or compilation error."
                                _state.value = MathSolverState.Error(error)
                            }
                            WorkInfo.State.CANCELLED -> {
                                _state.value = MathSolverState.Idle
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("MathSolver observeJob cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error observing MathSolverWorker")
            }
        }
    }

    fun cancelSolving(context: Context) {
        observeJob?.cancel()
        observeJob = null
        isBackgroundMode = false
        currentProblemText = ""
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("MathSolver_Active")
            workManager.cancelAllWorkByTag("MathSolverTag")
        } catch (e: Exception) {
            Timber.w(e, "Error cancelling MathSolver work")
        }
        _state.value = MathSolverState.Idle
    }

    fun resetState() {
        observeJob?.cancel()
        observeJob = null
        isBackgroundMode = false
        currentProblemText = ""
        _state.value = MathSolverState.Idle
    }
}
