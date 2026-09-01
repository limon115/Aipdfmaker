import re

with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "r") as f:
    content = f.read()

# Add imports
imports = """import kotlinx.serialization.json.Json
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
"""
content = content.replace("import kotlinx.serialization.json.Json", imports)

# update method signatures
content = content.replace("private fun createForegroundInfo(progressText: String, progress: Int, total: Int): ForegroundInfo {", "private fun createForegroundInfo(progressText: String, progress: Int, total: Int, projectId: Int): ForegroundInfo {")
content = content.replace("private fun showCompletedNotification(message: String) {", "private fun showCompletedNotification(message: String, projectId: Int) {")

# update method calls
content = content.replace("setForeground(createForegroundInfo(\"Starting generation...\", 0, totalTopics))", "setForeground(createForegroundInfo(\"Starting generation...\", 0, totalTopics, projectId))")
content = content.replace("setForeground(createForegroundInfo(\"Generating: ${topic.title}\", index, totalTopics))", "setForeground(createForegroundInfo(\"Generating: ${topic.title}\", index, totalTopics, projectId))")
content = content.replace("showCompletedNotification(\"Generation completed for ${project?.title ?: \"Project\"}\")", "showCompletedNotification(\"Generation completed for ${project?.title ?: \"Project\"}\", projectId)")
content = content.replace("showCompletedNotification(\"Generation failed: ${e.message}\")", "showCompletedNotification(\"Generation failed: ${e.message}\", projectId)")

# update notification builder in createForegroundInfo
builder1 = """val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Generating Study Notes")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setProgress(total, progress, false)
            .build()"""

new_builder1 = """val intent = Intent(Intent.ACTION_VIEW, Uri.parse("docmorph://note_generation/$projectId"), context, com.example.MainActivity::class.java).apply {
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
            .build()"""

content = content.replace(builder1, new_builder1)

# update notification builder in showCompletedNotification
builder2 = """val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("DocMorph Note Generation")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()"""

new_builder2 = """val intent = Intent(Intent.ACTION_VIEW, Uri.parse("docmorph://notes_viewer/$projectId"), context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, projectId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("DocMorph Note Generation")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()"""
            
content = content.replace(builder2, new_builder2)

with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "w") as f:
    f.write(content)
