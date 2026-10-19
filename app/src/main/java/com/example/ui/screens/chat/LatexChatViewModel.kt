package com.example.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.models.chat.ChatMessage
import com.example.domain.models.chat.ChatPdfStatus
import com.example.domain.services.chat.LatexChatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LatexChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    val suggestionChips = listOf(
        "Quantum Physics formula sheet with Dirac notation",
        "1-Page Modern Software Engineer Resume",
        "Calculus Exam with 4 problems & step-by-step solutions",
        "Formal Leave Application Letter template",
        "Comparison table with TikZ checkmarks",
        "Thermodynamics notes with key equations"
    )

    fun sendMessage(context: Context, prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank() || _isGenerating.value) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmedPrompt
        )

        val botMessageId = UUID.randomUUID().toString()
        val initialBotMessage = ChatMessage(
            id = botMessageId,
            isUser = false,
            text = "Analyzing your request and crafting your custom LaTeX document...",
            pdfStatus = ChatPdfStatus.IDLE
        )

        val currentList = _messages.value
        _messages.value = currentList + userMessage + initialBotMessage
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.Main) {
            val service = LatexChatService(context)
            try {
                val result = service.generateChatResponse(
                    history = currentList,
                    userPrompt = trimmedPrompt
                )

                // Update bot message with conversational text and extracted LaTeX
                updateMessage(botMessageId) {
                    it.copy(
                        text = result.conversationalText,
                        latexCode = result.latexCode,
                        pdfTitle = result.suggestedTitle,
                        pdfStatus = if (result.latexCode != null) ChatPdfStatus.COMPILING else ChatPdfStatus.IDLE
                    )
                }

                // If LaTeX code was generated, trigger PDF compilation
                if (result.latexCode != null) {
                    val compileResult = service.compileLatexToPdf(result.latexCode, result.suggestedTitle)
                    if (compileResult.isSuccess) {
                        val pdfFile = compileResult.getOrNull()
                        updateMessage(botMessageId) {
                            it.copy(
                                pdfFile = pdfFile,
                                pdfStatus = ChatPdfStatus.READY
                            )
                        }
                    } else {
                        val err = compileResult.exceptionOrNull()?.message ?: "Compilation error"
                        updateMessage(botMessageId) {
                            it.copy(
                                pdfStatus = ChatPdfStatus.ERROR,
                                errorMessage = err
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                updateMessage(botMessageId) {
                    it.copy(
                        text = "I encountered an issue generating your document: ${e.localizedMessage ?: "Unknown error"}",
                        pdfStatus = ChatPdfStatus.ERROR,
                        errorMessage = e.message
                    )
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun recompileLatex(context: Context, messageId: String, newLatex: String) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        val docTitle = msg.pdfTitle ?: "Latex Document"

        updateMessage(messageId) {
            it.copy(
                latexCode = newLatex,
                pdfStatus = ChatPdfStatus.COMPILING,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            val service = LatexChatService(context)
            try {
                val compileResult = service.compileLatexToPdf(newLatex, docTitle)
                if (compileResult.isSuccess) {
                    val pdfFile = compileResult.getOrNull()
                    updateMessage(messageId) {
                        it.copy(
                            pdfFile = pdfFile,
                            pdfStatus = ChatPdfStatus.READY
                        )
                    }
                } else {
                    val err = compileResult.exceptionOrNull()?.message ?: "Compilation error"
                    updateMessage(messageId) {
                        it.copy(
                            pdfStatus = ChatPdfStatus.ERROR,
                            errorMessage = err
                        )
                    }
                }
            } catch (e: Exception) {
                updateMessage(messageId) {
                    it.copy(
                        pdfStatus = ChatPdfStatus.ERROR,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun startEditingMessage(message: ChatMessage) {
        _editingMessage.value = message
    }

    fun stopEditingMessage() {
        _editingMessage.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) transform(msg) else msg
        }
    }
}
