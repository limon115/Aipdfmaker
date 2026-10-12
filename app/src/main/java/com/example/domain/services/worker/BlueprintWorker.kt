package com.example.domain.services.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.example.data.database.AppDatabase
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.services.ai.BlueprintService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class BlueprintWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val projectId = inputData.getInt("project_id", -1)
        if (projectId == -1) {
            val errorMsg = "Invalid project ID"
            Timber.e("BlueprintWorker: $errorMsg")
            com.example.utils.AppLogger.e("BlueprintWorker", errorMsg)
            return Result.failure(androidx.work.workDataOf("error" to errorMsg))
        }

        try {
            // Fetch project from DB
            val projectDao = AppDatabase.getDatabase(context).projectDao()
            val project = projectDao.getProjectById(projectId)
            val extractedText = project?.sourceText ?: ""

            if (extractedText.isBlank()) {
                val errorMsg = "No source text found for this project. Please import or enter notes first."
                Timber.e("BlueprintWorker: $errorMsg")
                com.example.utils.AppLogger.e("BlueprintWorker", errorMsg)
                return Result.failure(androidx.work.workDataOf("error" to errorMsg))
            }

            // Init AI client via robust candidate resolution
            val dataStore = AiSettingsDataStore(context)
            val settings = dataStore.aiSettingsFlow.first()
            val candidates = settings.resolveCandidates(primarySlot = 1)

            var summary: com.example.domain.models.BlueprintSummary? = null

            if (candidates.isEmpty()) {
                com.example.utils.AppLogger.w("BlueprintWorker", "No AI API key configured for Blueprint Builder. Generating smart offline structured blueprint.")
                summary = BlueprintService.generateFallbackBlueprint(extractedText)
            } else {
                var lastError: Exception? = null

                for (candidate in candidates) {
                    try {
                        com.example.utils.AppLogger.i("BlueprintWorker", "Attempting blueprint generation via ${candidate.provider} (${candidate.model})")
                        val aiClient = AiNetworkClient(
                            provider = candidate.provider,
                            apiKey = candidate.apiKey,
                            model = candidate.model,
                            temperature = candidate.temperature,
                            featureName = "Knowledge Blueprint"
                        )
                        val blueprintService = BlueprintService(aiClient)
                        val result = blueprintService.generateBlueprint(extractedText)

                        if (result.isSuccess && result.getOrNull() != null) {
                            summary = result.getOrNull()
                            com.example.utils.AppLogger.i("BlueprintWorker", "Successfully generated blueprint via ${candidate.provider}")
                            break
                        } else {
                            lastError = result.exceptionOrNull() as? Exception ?: Exception("Generation failed")
                            com.example.utils.AppLogger.w("BlueprintWorker", "Candidate ${candidate.provider} failed: ${lastError.message}")
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        com.example.utils.AppLogger.w("BlueprintWorker", "Exception in candidate ${candidate.provider}: ${e.message}, trying next candidate")
                    }
                }

                // If all AI candidates fail (e.g. invalid key, quota limit, network unreachable), gracefully fallback!
                if (summary == null) {
                    com.example.utils.AppLogger.w("BlueprintWorker", "All AI candidates failed (${lastError?.message}). Using smart offline structured blueprint fallback.")
                    summary = BlueprintService.generateFallbackBlueprint(extractedText)
                }
            }

            if (summary != null) {
                val jsonFormat = Json { ignoreUnknownKeys = true; encodeDefaults = true }
                val summaryJson = jsonFormat.encodeToString(summary)
                
                // Save JSON to file to avoid 10KB WorkManager crash
                val tempFile = java.io.File(context.cacheDir, "blueprint_${projectId}.json")
                tempFile.writeText(summaryJson)

                val outputData = Data.Builder()
                    .putString("blueprint_file", tempFile.name)
                    .build()
                    
                return Result.success(outputData)
            } else {
                val errorMsg = "Failed to construct blueprint from source text."
                com.example.utils.AppLogger.e("BlueprintWorker", errorMsg)
                return Result.failure(androidx.work.workDataOf("error" to errorMsg))
            }

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown exception during blueprint generation"
            com.example.utils.AppLogger.e("BlueprintWorker", "Exception during blueprint generation", e)
            return Result.failure(androidx.work.workDataOf("error" to errorMsg))
        }
    }
}
