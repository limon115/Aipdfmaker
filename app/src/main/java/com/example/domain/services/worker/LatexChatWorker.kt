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
import com.example.data.repository.LatexChatRepository
import com.example.domain.models.chat.ChatPdfStatus
import com.example.domain.services.chat.LatexChatService
import com.example.domain.services.pdf.PdfNotificationManager
import com.example.utils.AppLogger
import java.io.File

class LatexChatWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SESSION_ID = "KEY_SESSION_ID"
        const val KEY_PROMPT = "KEY_PROMPT"
        const val KEY_BOT_MSG_ID = "KEY_BOT_MSG_ID"
        const val KEY_PDF_PATH = "KEY_PDF_PATH"
        const val KEY_ERROR = "KEY_ERROR"
        const val CHANNEL_ID = "latex_chat_bg_channel"
        const val NOTIFICATION_ID = 5002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: ""
        val prompt = inputData.getString(KEY_PROMPT) ?: ""
        val botMsgId = inputData.getString(KEY_BOT_MSG_ID) ?: ""

        if (prompt.isBlank() || sessionId.isBlank()) {
            val err = "Prompt or session ID was missing."
            return Result.failure(workDataOf(KEY_ERROR to err))
        }

        try {
            setProgress(workDataOf("PROGRESS" to 0.25f, "STATUS" to "AI is analyzing requirements & drafting LaTeX..."))
            setForeground(createForegroundInfo("Drafting your custom LaTeX document..."))
        } catch (e: Exception) {
            AppLogger.w("LatexChatWorker", "Failed to set foreground status: ${e.message}")
        }

        val repository = LatexChatRepository(context)
        val chatService = LatexChatService(context)

        return try {
            val history = repository.getMessages(sessionId)
            val result = chatService.generateChatResponse(history, prompt)

            // Update bot message with conversational text and code
            repository.updateMessage(sessionId, botMsgId) {
                it.copy(
                    text = result.conversationalText,
                    latexCode = result.latexCode,
                    pdfTitle = result.suggestedTitle,
                    pdfStatus = if (result.latexCode != null) ChatPdfStatus.COMPILING else ChatPdfStatus.IDLE
                )
            }

            if (result.latexCode != null) {
                try {
                    setProgress(workDataOf("PROGRESS" to 0.70f, "STATUS" to "Compiling LaTeX document to PDF via XeLaTeX..."))
                    setForeground(createForegroundInfo("Compiling document into vector PDF..."))
                } catch (_: Exception) {}

                val compileResult = chatService.compileLatexToPdf(result.latexCode, result.suggestedTitle)
                if (compileResult.isSuccess) {
                    val pdfFile = compileResult.getOrNull()
                    if (pdfFile != null && pdfFile.exists()) {
                        repository.updateMessage(sessionId, botMsgId) {
                            it.copy(
                                pdfFile = pdfFile,
                                pdfStatus = ChatPdfStatus.READY
                            )
                        }
                        showSuccessNotification(result.suggestedTitle, pdfFile)
                        Result.success(workDataOf(KEY_PDF_PATH to pdfFile.absolutePath))
                    } else {
                        val err = "PDF file was not produced."
                        repository.updateMessage(sessionId, botMsgId) {
                            it.copy(pdfStatus = ChatPdfStatus.ERROR, errorMessage = err)
                        }
                        showErrorNotification(result.suggestedTitle, err)
                        Result.failure(workDataOf(KEY_ERROR to err))
                    }
                } else {
                    val err = compileResult.exceptionOrNull()?.message ?: "Compilation error"
                    repository.updateMessage(sessionId, botMsgId) {
                        it.copy(pdfStatus = ChatPdfStatus.ERROR, errorMessage = err)
                    }
                    showErrorNotification(result.suggestedTitle, err)
                    Result.failure(workDataOf(KEY_ERROR to err))
                }
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Generation failed"
            AppLogger.e("LatexChatWorker", "Worker exception: $err", e)
            repository.updateMessage(sessionId, botMsgId) {
                it.copy(
                    text = "Failed to complete in background: $err",
                    pdfStatus = ChatPdfStatus.ERROR,
                    errorMessage = err
                )
            }
            showErrorNotification("LaTeX AI Assistant", err)
            Result.failure(workDataOf(KEY_ERROR to err))
        }
    }

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("LaTeX AI Assistant (Background)")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showSuccessNotification(title: String, pdfFile: File) {
        val pdfNotificationManager = PdfNotificationManager(context)
        pdfNotificationManager.showSuccessNotification(title, pdfFile)
    }

    private fun showErrorNotification(title: String, errorMessage: String) {
        val pdfNotificationManager = PdfNotificationManager(context)
        pdfNotificationManager.showErrorNotification(title, errorMessage)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LaTeX AI Background Worker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of background LaTeX document generation and PDF compilation"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
