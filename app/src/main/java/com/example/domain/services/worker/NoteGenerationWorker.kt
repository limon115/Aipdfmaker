package com.example.domain.services.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.database.AppDatabase
import com.example.data.database.DocumentSnippetEntity
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.models.BlueprintSummary
import com.example.domain.repository.LatexCompilerRepository
import com.example.domain.services.ai.NoteGenerationService
import com.example.domain.services.ai.TextChunker
import com.example.domain.services.ai.TopicContextRetriever
import com.example.data.cache.AiResponseCache
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import java.io.File
import android.os.Environment


class NoteGenerationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val PROGRESS = "Progress"
        const val TOTAL = "Total"
        const val CURRENT_TOPIC = "CurrentTopic"
        const val ERROR = "Error"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "NoteGenerationChannel"

    init {
        createNotificationChannel()
    }

    override suspend fun doWork(): Result {
        val projectId = inputData.getInt("PROJECT_ID", -1)
        val blueprintJson = inputData.getString("BLUEPRINT_JSON") ?: ""

        if (projectId == -1 || blueprintJson.isEmpty()) {
            return Result.failure(workDataOf(ERROR to "Invalid Input Data"))
        }

        val blueprint = try {
            Json.decodeFromString<BlueprintSummary>(blueprintJson)
        } catch (e: Exception) {
            return Result.failure(workDataOf(ERROR to "Failed to parse blueprint"))
        }

        val db = AppDatabase.getDatabase(context)
        val projectDao = db.projectDao()
        val project = projectDao.getProjectById(projectId)
        val sourceText = project?.sourceText ?: ""

        if (sourceText.isEmpty()) {
            return Result.failure(workDataOf(ERROR to "Source text is empty"))
        }

        val dataStore = AiSettingsDataStore(context)
        val settings = dataStore.aiSettingsFlow.first()
        val dummyClient = AiNetworkClient(
            provider = settings.ai2Provider.name,
            apiKey = settings.ai2ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY },
            model = settings.ai2Model.ifBlank { "gemini-1.5-flash" },
            temperature = settings.ai2Temperature
        )
        val cache = AiResponseCache(context)
        val service = NoteGenerationService(dummyClient, cache)
        val snippetDao = db.documentSnippetDao()
        val totalTopics = blueprint.topics.size

        try {
            setForeground(createForegroundInfo("Starting generation...", 0, totalTopics, projectId))
            
            val chunker = TextChunker()
            val retriever = TopicContextRetriever()
            val chunks = chunker.chunkText(sourceText, 3000, 300)
            
            val existingSnippets = snippetDao.getSnippetsForProject(projectId).first()
            val completedTopics = existingSnippets.map { it.topicTitle }.toSet()
            
            blueprint.topics.forEachIndexed { index, topic ->
                if (completedTopics.contains(topic.title)) {
                    // Skip already generated topic (Resumability)
                    setProgress(workDataOf(PROGRESS to index, TOTAL to totalTopics, CURRENT_TOPIC to "${topic.title} (Cached)"))
                    return@forEachIndexed
                }

                setProgress(workDataOf(PROGRESS to index, TOTAL to totalTopics, CURRENT_TOPIC to topic.title))
                setForeground(createForegroundInfo("Generating: ${topic.title}", index, totalTopics, projectId))

                // Retrieve only relevant chunks for this topic
                val relevantContextForTopic = retriever.retrieveContext(topic.title, chunks, 8000)
                
                val html = service.generateDocumentForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    relevantContext = relevantContextForTopic,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-1.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY },
                    ai2Temperature = settings.ai2Temperature
                )

                val snippet = DocumentSnippetEntity(
                    projectId = projectId,
                    topicTitle = topic.title,
                    jsonContent = html,
                    orderIndex = index
                )
                snippetDao.insertSnippet(snippet)
            }
            
            // Combine all snippets and write main.tex to project folder
            if (project != null) {
                try {
                    val allSnippets = snippetDao.getSnippetsForProject(projectId).first()
                    val masterLatex = allSnippets.joinToString("\n\n") { it.jsonContent }
                    
                    val safeName = project.title
                        .trim()
                        .replace(Regex("[^a-zA-Z0-9.-]"), "_")
                        .ifEmpty { "Project" }
                    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val baseDir = File(documentsDir, "aipdfs/$safeName")
                    
                    if (!baseDir.exists()) {
                        baseDir.mkdirs()
                    }
                    
                    val compilerRepo = LatexCompilerRepository(context)
                    val fullLatex = compilerRepo.buildFullLatex(project, masterLatex)
                    
                    val texFile = File(baseDir, "main.tex")
                    texFile.writeText(fullLatex, Charsets.UTF_8)
                    com.example.utils.AppLogger.i("NoteGenWorker", "Successfully wrote full main.tex to ${texFile.absolutePath}")
                } catch (e: Exception) {
                    com.example.utils.AppLogger.e("NoteGenWorker", "Failed to write main.tex", e)
                }
            }
            
            // Mark project as Completed
            project?.let { projectDao.updateProject(it.copy(status = "Completed", lastUpdated = System.currentTimeMillis())) }
            
            setProgress(workDataOf(PROGRESS to totalTopics, TOTAL to totalTopics, CURRENT_TOPIC to "Finished"))
            showCompletedNotification("Generation completed for ${project?.title ?: "Project"}", projectId)
            com.example.utils.AppLogger.i("NoteGenWorker", "Finished generation for project $projectId")
            return Result.success()
        } catch (e: Exception) {
            com.example.utils.AppLogger.e("NoteGenWorker", "Fatal error in worker", e)
            e.printStackTrace()
            // Mark project as Failed
            project?.let { projectDao.updateProject(it.copy(status = "Failed", lastUpdated = System.currentTimeMillis())) }
            showCompletedNotification("Generation failed: ${e.message}", projectId)
            return Result.failure(workDataOf(ERROR to (e.message ?: "Unknown error")))
        }
    }

    private fun createForegroundInfo(progressText: String, progress: Int, total: Int, projectId: Int): ForegroundInfo {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("docmorph://note_generation/$projectId"), context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, projectId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Generating Study Notes")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setProgress(total, progress, false)
            .setContentIntent(pendingIntent)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, notification)
        }
    }

    private fun showCompletedNotification(message: String, projectId: Int) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("docmorph://notes_viewer/$projectId"), context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, projectId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("DocMorph Note Generation")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Note Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of note generation"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
