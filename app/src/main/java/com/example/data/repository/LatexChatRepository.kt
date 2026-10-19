package com.example.data.repository

import android.content.Context
import com.example.domain.models.chat.ChatMessage
import com.example.domain.models.chat.ChatPdfStatus
import com.example.domain.models.chat.ChatSession
import com.example.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LatexChatRepository(private val context: Context) {

    private val baseDir = File(context.filesDir, "latex_chats").apply {
        if (!exists()) mkdirs()
    }
    private val sessionsIndexFile = File(baseDir, "sessions_index.json")
    private val mutex = Mutex()

    suspend fun getAllSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readSessionsIndex()
        }
    }

    suspend fun getSession(sessionId: String): ChatSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readSessionsIndex().find { it.id == sessionId }
        }
    }

    suspend fun createSession(title: String = "New Chat"): ChatSession = withContext(Dispatchers.IO) {
        mutex.withLock {
            val session = ChatSession(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "New Chat" },
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis(),
                previewText = "Tap to start conversation..."
            )
            val current = readSessionsIndex().toMutableList()
            current.add(0, session)
            writeSessionsIndex(current)
            session
        }
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sessions = readSessionsIndex().toMutableList()
            val idx = sessions.indexOfFirst { it.id == sessionId }
            if (idx != -1) {
                sessions[idx] = sessions[idx].copy(
                    title = newTitle.trim().ifBlank { "Untitled Chat" },
                    lastUpdatedAt = System.currentTimeMillis()
                )
                writeSessionsIndex(sessions)
            }
        }
    }

    suspend fun updateSessionPreview(sessionId: String, previewText: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sessions = readSessionsIndex().toMutableList()
            val idx = sessions.indexOfFirst { it.id == sessionId }
            if (idx != -1) {
                sessions[idx] = sessions[idx].copy(
                    previewText = previewText.take(80),
                    lastUpdatedAt = System.currentTimeMillis()
                )
                // Move updated session to top
                val updated = sessions.removeAt(idx)
                sessions.add(0, updated)
                writeSessionsIndex(sessions)
            }
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sessions = readSessionsIndex().toMutableList()
            sessions.removeAll { it.id == sessionId }
            writeSessionsIndex(sessions)

            val sessionFile = File(baseDir, "session_${sessionId}.json")
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
        }
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        mutex.withLock {
            baseDir.listFiles()?.forEach { it.delete() }
            writeSessionsIndex(emptyList())
        }
    }

    suspend fun getMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readMessagesFile(sessionId)
        }
    }

    suspend fun saveMessages(sessionId: String, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeMessagesFile(sessionId, messages)

            // Update session preview & timestamp
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null) {
                val sessions = readSessionsIndex().toMutableList()
                val idx = sessions.indexOfFirst { it.id == sessionId }
                if (idx != -1) {
                    val preview = if (!lastMsg.latexCode.isNullOrBlank()) {
                        "📄 ${lastMsg.text.take(60)}"
                    } else {
                        lastMsg.text.take(60)
                    }
                    val updated = sessions[idx].copy(
                        previewText = preview,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                    sessions.removeAt(idx)
                    sessions.add(0, updated)
                    writeSessionsIndex(sessions)
                }
            }
        }
    }

    suspend fun addMessage(sessionId: String, message: ChatMessage) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readMessagesFile(sessionId).toMutableList()
            current.add(message)
            writeMessagesFile(sessionId, current)

            // Update preview in sessions index
            val sessions = readSessionsIndex().toMutableList()
            val idx = sessions.indexOfFirst { it.id == sessionId }
            if (idx != -1) {
                val preview = message.text.take(60)
                val updated = sessions[idx].copy(
                    previewText = preview,
                    lastUpdatedAt = System.currentTimeMillis()
                )
                sessions.removeAt(idx)
                sessions.add(0, updated)
                writeSessionsIndex(sessions)
            }
        }
    }

    suspend fun updateMessage(
        sessionId: String,
        messageId: String,
        transform: (ChatMessage) -> ChatMessage
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readMessagesFile(sessionId).toMutableList()
            val idx = current.indexOfFirst { it.id == messageId }
            if (idx != -1) {
                current[idx] = transform(current[idx])
                writeMessagesFile(sessionId, current)
            }
        }
    }

    suspend fun exportSessionAsMarkdown(sessionId: String): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val session = readSessionsIndex().find { it.id == sessionId }
            val messages = readMessagesFile(sessionId)
            val title = session?.title ?: "LaTeX AI Conversation"
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(session?.createdAt ?: System.currentTimeMillis()))

            val sb = StringBuilder("# $title\n\n")
            sb.append("*Generated by DocMorph LaTeX AI ChatBot • $dateStr*\n\n---\n\n")

            for (msg in messages) {
                val sender = if (msg.isUser) "**User**" else "**DocMorph AI**"
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                sb.append("### $sender ($time)\n\n")
                sb.append("${msg.text}\n\n")
                if (!msg.latexCode.isNullOrBlank()) {
                    sb.append("```latex\n${msg.latexCode}\n```\n\n")
                }
                if (msg.pdfFile != null && msg.pdfFile.exists()) {
                    sb.append("> 📄 Generated PDF: `${msg.pdfFile.name}` (${com.example.utils.FileUtils.formatFileSize(msg.pdfFile.length())})\n\n")
                }
                sb.append("---\n\n")
            }
            sb.toString()
        }
    }

    // --- Private JSON serialization helpers ---

    private fun readSessionsIndex(): List<ChatSession> {
        if (!sessionsIndexFile.exists()) return emptyList()
        return try {
            val text = sessionsIndexFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(text)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChatSession(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "New Chat"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastUpdatedAt = obj.optLong("lastUpdatedAt", System.currentTimeMillis()),
                        previewText = obj.optString("previewText", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            AppLogger.w("LatexChatRepository", "Error reading sessions index: ${e.message}")
            emptyList()
        }
    }

    private fun writeSessionsIndex(sessions: List<ChatSession>) {
        try {
            val jsonArray = JSONArray()
            for (s in sessions) {
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("createdAt", s.createdAt)
                    put("lastUpdatedAt", s.lastUpdatedAt)
                    put("previewText", s.previewText)
                }
                jsonArray.put(obj)
            }
            sessionsIndexFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.e("LatexChatRepository", "Error writing sessions index: ${e.message}", e)
        }
    }

    private fun readMessagesFile(sessionId: String): List<ChatMessage> {
        val file = File(baseDir, "session_${sessionId}.json")
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(text)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pdfPath = if (obj.isNull("pdfPath")) null else obj.optString("pdfPath")
                val statusStr = obj.optString("pdfStatus", "IDLE")
                val status = runCatching { ChatPdfStatus.valueOf(statusStr) }.getOrDefault(ChatPdfStatus.IDLE)

                list.add(
                    ChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        sessionId = sessionId,
                        isUser = obj.optBoolean("isUser", false),
                        text = obj.optString("text", ""),
                        latexCode = if (obj.isNull("latexCode")) null else obj.optString("latexCode"),
                        pdfFile = pdfPath?.let { File(it) },
                        pdfTitle = if (obj.isNull("pdfTitle")) null else obj.optString("pdfTitle"),
                        pdfStatus = status,
                        errorMessage = if (obj.isNull("errorMessage")) null else obj.optString("errorMessage"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            AppLogger.w("LatexChatRepository", "Error reading messages for $sessionId: ${e.message}")
            emptyList()
        }
    }

    private fun writeMessagesFile(sessionId: String, messages: List<ChatMessage>) {
        val file = File(baseDir, "session_${sessionId}.json")
        try {
            val jsonArray = JSONArray()
            for (m in messages) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("sessionId", sessionId)
                    put("isUser", m.isUser)
                    put("text", m.text)
                    put("latexCode", m.latexCode ?: JSONObject.NULL)
                    put("pdfPath", m.pdfFile?.absolutePath ?: JSONObject.NULL)
                    put("pdfTitle", m.pdfTitle ?: JSONObject.NULL)
                    put("pdfStatus", m.pdfStatus.name)
                    put("errorMessage", m.errorMessage ?: JSONObject.NULL)
                    put("timestamp", m.timestamp)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.e("LatexChatRepository", "Error writing messages for $sessionId: ${e.message}", e)
        }
    }
}
