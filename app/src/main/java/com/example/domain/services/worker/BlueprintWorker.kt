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

            // Init AI client
            val dataStore = AiSettingsDataStore(context)
            val settings = dataStore.aiSettingsFlow.first()
            val aiClient = AiNetworkClient(
                provider = settings.ai1Provider.name,
                apiKey = settings.ai1ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY },
                model = settings.ai1Model.ifBlank { "gemini-2.5-flash" },
                temperature = settings.ai2Temperature
            )
            val blueprintService = BlueprintService(aiClient)

            val result = blueprintService.generateBlueprint(extractedText)

            if (result.isSuccess) {
                val summary = result.getOrNull()
                val jsonFormat = Json { ignoreUnknownKeys = true; encodeDefaults = true }
                val summaryJson = summary?.let { jsonFormat.encodeToString(it) } ?: ""
                
                // 🛡️ FIX 1: Save JSON to file to avoid 10KB WorkManager crash
                val tempFile = java.io.File(context.cacheDir, "blueprint_${projectId}.json")
                tempFile.writeText(summaryJson)

                val outputData = Data.Builder()
                    .putString("blueprint_file", tempFile.name)
                    .build()
                    
                return Result.success(outputData)
            } else {
                Timber.e("BlueprintWorker: Service failed to generate blueprint")
                return Result.failure()
            }

        } catch (e: Exception) {
            Timber.e(e, "BlueprintWorker: Exception during blueprint generation")
            return Result.failure()
        }
    }
}
