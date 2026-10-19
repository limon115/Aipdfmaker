package com.example.domain.models.chat

import java.io.File
import java.util.UUID

enum class ChatPdfStatus {
    IDLE,
    COMPILING,
    READY,
    ERROR
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val previewText: String = ""
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val isUser: Boolean,
    val text: String,
    val latexCode: String? = null,
    val pdfFile: File? = null,
    val pdfTitle: String? = null,
    val pdfStatus: ChatPdfStatus = ChatPdfStatus.IDLE,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
