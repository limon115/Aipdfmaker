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

data class PromptCategory(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val prompts: List<PromptItem>
)

data class PromptItem(
    val title: String,
    val description: String,
    val promptText: String
)

class LatexChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatingStatus = MutableStateFlow("Thinking...")
    val generatingStatus: StateFlow<String> = _generatingStatus.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

    val promptCategories = listOf(
        PromptCategory(
            id = "math_physics",
            title = "Math & Physics",
            iconEmoji = "📐",
            prompts = listOf(
                PromptItem(
                    title = "Quantum Physics Sheet",
                    description = "Formulas, bra-ket notation, and Schrödinger eq",
                    promptText = "Create a comprehensive 1-page Quantum Mechanics formula cheat sheet with Dirac notation, operators, and particle-in-a-box equations."
                ),
                PromptItem(
                    title = "Calculus Exam with Solutions",
                    description = "4 integration problems with step-by-step proofs",
                    promptText = "Create a university-level Calculus exam paper with 4 challenging integral problems and complete step-by-step worked solutions."
                ),
                PromptItem(
                    title = "Differential Equations",
                    description = "First & second order ODEs with boundary conditions",
                    promptText = "Generate a reference sheet for second-order linear differential equations with characteristic roots and boundary value examples."
                ),
                PromptItem(
                    title = "Electromagnetism Maxwell Equations",
                    description = "Vector calculus & Maxwell's 4 laws",
                    promptText = "Write a clear mathematical summary of Maxwell's Equations in both differential and integral forms with physical interpretations."
                )
            )
        ),
        PromptCategory(
            id = "resumes_docs",
            title = "Resumes & Docs",
            iconEmoji = "💼",
            prompts = listOf(
                PromptItem(
                    title = "Modern Tech Resume",
                    description = "1-page clean CV with skills, experience & projects",
                    promptText = "Create a modern, elegant 1-page Software Engineer Resume template with Experience, Education, Technical Skills, and Projects sections."
                ),
                PromptItem(
                    title = "Formal Leave Application",
                    description = "Professional academic leave letter to Dean/Principal",
                    promptText = "Draft a formal academic leave application letter to the University Principal/Dean with polite phrasing and structured date/subject fields."
                ),
                PromptItem(
                    title = "Research Paper Template",
                    description = "Two-column academic article with abstract & BibTeX",
                    promptText = "Create a two-column IEEE/ACM-style academic paper template complete with Abstract, Introduction, Methodology, and References."
                ),
                PromptItem(
                    title = "Professional Cover Letter",
                    description = "Tailored job application letter for senior developer",
                    promptText = "Generate a professional, polished cover letter template for a senior developer applying to an innovative engineering team."
                )
            )
        ),
        PromptCategory(
            id = "diagrams_tables",
            title = "Tables & TikZ",
            iconEmoji = "📊",
            prompts = listOf(
                PromptItem(
                    title = "Comparison Table with TikZ",
                    description = "Colored checkmarks and formatted columns",
                    promptText = "Create a professional comparison table using booktabs with checkmarks, feature columns, and colored status highlights."
                ),
                PromptItem(
                    title = "Physics Optics Ray Diagram",
                    description = "Convex lens with focal points and ray paths in TikZ",
                    promptText = "Draw a clean TikZ physics optics ray diagram showing a convex lens, object, focal points (F, 2F), and refracted light rays."
                ),
                PromptItem(
                    title = "RLC Circuit Schematic",
                    description = "Circuitikz electrical diagram with source & filter",
                    promptText = "Generate an electrical circuit schematic using circuitikz showing an AC voltage source, resistor, capacitor, and inductor."
                ),
                PromptItem(
                    title = "Periodic Chemical Reactions",
                    description = "Summary table of periodic table oxidation trends",
                    promptText = "Create a structured chemical reaction and periodic trend summary table with reaction equations and state symbols."
                )
            )
        )
    )

    fun selectCategory(index: Int) {
        _selectedCategoryIndex.value = index.coerceIn(0, promptCategories.size - 1)
    }

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
        _generatingStatus.value = "Analyzing prompt & drafting LaTeX..."

        viewModelScope.launch(Dispatchers.Main) {
            val service = LatexChatService(context)
            try {
                val result = service.generateChatResponse(
                    history = currentList,
                    userPrompt = trimmedPrompt
                )

                _generatingStatus.value = "Compiling document to PDF via XeLaTeX..."

                updateMessage(botMessageId) {
                    it.copy(
                        text = result.conversationalText,
                        latexCode = result.latexCode,
                        pdfTitle = result.suggestedTitle,
                        pdfStatus = if (result.latexCode != null) ChatPdfStatus.COMPILING else ChatPdfStatus.IDLE
                    )
                }

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
                _generatingStatus.value = "Thinking..."
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

    fun retryGeneration(context: Context, botMessageId: String) {
        val msgList = _messages.value
        val botIdx = msgList.indexOfFirst { it.id == botMessageId }
        if (botIdx <= 0) return

        val priorUserMsg = msgList[botIdx - 1]
        if (!priorUserMsg.isUser) return

        // Remove the failed bot message
        _messages.value = msgList.filter { it.id != botMessageId }
        // Re-send the prompt
        sendMessage(context, priorUserMsg.text)
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
