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
            Timber.e("BlueprintWorker: Invalid project ID")
            return Result.failure()
        }

        try {
            // Fetch project from DB
            val projectDao = AppDatabase.getDatabase(context).projectDao()
            val project = projectDao.getProjectById(projectId)
            val extractedText = project?.sourceText ?: ""

            if (extractedText.isBlank()) {
                Timber.e("BlueprintWorker: Extracted text is empty")
                return Result.failure()
            }

            // Init AI client via robust candidate resolution
            val dataStore = AiSettingsDataStore(context)
            val settings = dataStore.aiSettingsFlow.first()
            val candidates = com.example.data.datastore.resolveCandidates(settings, primarySlot = 1)

            if (candidates.isEmpty()) {
                val errorMsg = "No AI API key configured. Please configure your API key in AI Settings."
                Timber.e("BlueprintWorker: $errorMsg")
                return Result.failure(androidx.work.workDataOf("error" to errorMsg))
            }

            var summary: com.example.domain.models.BlueprintSummary? = null
            var lastError: Exception? = null

            for (candidate in candidates) {
                try {
                    Timber.i("BlueprintWorker: Attempting blueprint generation via ${candidate.provider} (${candidate.model})")
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
                        Timber.i("BlueprintWorker: Successfully generated blueprint via ${candidate.provider}")
                        break
                    } else {
                        lastError = result.exceptionOrNull() as? Exception ?: Exception("Generation failed")
                        Timber.w("BlueprintWorker: Candidate ${candidate.provider} failed: ${lastError.message}")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    Timber.w(e, "BlueprintWorker: Exception in candidate ${candidate.provider}, trying next candidate")
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
                val errorMsg = lastError?.message ?: "AI failed to generate blueprint from source text."
                Timber.e("BlueprintWorker: All candidates failed. $errorMsg")
                return Result.failure(androidx.work.workDataOf("error" to errorMsg))
            }

        } catch (e: Exception) {
            Timber.e(e, "BlueprintWorker: Exception during blueprint generation")
            return Result.failure(androidx.work.workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
