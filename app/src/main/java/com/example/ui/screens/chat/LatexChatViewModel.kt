package com.example.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.repository.LatexChatRepository
import com.example.domain.models.chat.ChatMessage
import com.example.domain.models.chat.ChatPdfStatus
import com.example.domain.models.chat.ChatSession
import com.example.domain.services.chat.LatexChatService
import com.example.domain.services.worker.LatexChatWorker
import com.example.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
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

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

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

    private var observeJob: Job? = null
    private var repository: LatexChatRepository? = null

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

    fun initialize(context: Context) {
        if (repository == null) {
            repository = LatexChatRepository(context)
        }
        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: return@launch
            val list = repo.getAllSessions()
            _sessions.value = list
            if (list.isNotEmpty() && _currentSession.value == null) {
                selectSession(context, list.first().id)
            } else if (list.isEmpty()) {
                startNewChat(context)
            }
        }
    }

    fun selectCategory(index: Int) {
        _selectedCategoryIndex.value = index.coerceIn(0, promptCategories.size - 1)
    }

    fun startNewChat(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            val newSession = repo.createSession("New Chat")
            _sessions.value = repo.getAllSessions()
            _currentSession.value = newSession
            _messages.value = emptyList()
            _isGenerating.value = false
            observeJob?.cancel()
        }
    }

    fun selectSession(context: Context, sessionId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            val session = repo.getSession(sessionId) ?: return@launch
            _currentSession.value = session
            _messages.value = repo.getMessages(sessionId)
            _isGenerating.value = false
            observeJob?.cancel()
        }
    }

    fun renameSession(context: Context, sessionId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            repo.updateSessionTitle(sessionId, newTitle)
            _sessions.value = repo.getAllSessions()
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = _currentSession.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteSession(context: Context, sessionId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            repo.deleteSession(sessionId)
            val updated = repo.getAllSessions()
            _sessions.value = updated
            if (_currentSession.value?.id == sessionId) {
                if (updated.isNotEmpty()) {
                    selectSession(context, updated.first().id)
                } else {
                    startNewChat(context)
                }
            }
        }
    }

    fun sendMessage(context: Context, prompt: String, runInBackground: Boolean = false) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank() || _isGenerating.value) return

        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            var session = _currentSession.value
            if (session == null) {
                session = repo.createSession(suggestSessionTitle(trimmedPrompt))
                _currentSession.value = session
                _sessions.value = repo.getAllSessions()
            }

            val sessionId = session.id
            val isFirstMessage = _messages.value.isEmpty()
            if (isFirstMessage && (session.title == "New Chat" || session.title.isBlank())) {
                val newTitle = suggestSessionTitle(trimmedPrompt)
                repo.updateSessionTitle(sessionId, newTitle)
                _currentSession.value = session.copy(title = newTitle)
                _sessions.value = repo.getAllSessions()
            }

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                isUser = true,
                text = trimmedPrompt
            )

            val botMessageId = UUID.randomUUID().toString()
            val initialBotMessage = ChatMessage(
                id = botMessageId,
                sessionId = sessionId,
                isUser = false,
                text = "Analyzing requirements and drafting your custom LaTeX document...",
                pdfStatus = ChatPdfStatus.IDLE
            )

            val newMessages = _messages.value + userMessage + initialBotMessage
            _messages.value = newMessages
            repo.saveMessages(sessionId, newMessages)

            _isGenerating.value = true
            _generatingStatus.value = "Analyzing prompt & drafting LaTeX..."

            // Enqueue WorkManager job for 100% reliable background execution
            enqueueBackgroundJob(context, sessionId, trimmedPrompt, botMessageId)
        }
    }

    private fun enqueueBackgroundJob(
        context: Context,
        sessionId: String,
        prompt: String,
        botMessageId: String
    ) {
        observeJob?.cancel()

        val workManager = WorkManager.getInstance(context)
        val workName = "LatexChat_${sessionId}_${botMsgIdClean(botMessageId)}"

        val inputData = Data.Builder()
            .putString(LatexChatWorker.KEY_SESSION_ID, sessionId)
            .putString(LatexChatWorker.KEY_PROMPT, prompt)
            .putString(LatexChatWorker.KEY_BOT_MSG_ID, botMessageId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<LatexChatWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("LatexChatTag")
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        observeJob = viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            try {
                workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collect { workInfo ->
                    if (workInfo != null) {
                        val status = workInfo.progress.getString("STATUS")
                        if (status != null) {
                            _generatingStatus.value = status
                        }

                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> {
                                _isGenerating.value = true
                                syncLatestMessages(repo, sessionId)
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _isGenerating.value = false
                                _generatingStatus.value = "Ready"
                                syncLatestMessages(repo, sessionId)
                            }
                            WorkInfo.State.FAILED -> {
                                _isGenerating.value = false
                                _generatingStatus.value = "Failed"
                                syncLatestMessages(repo, sessionId)
                            }
                            WorkInfo.State.CANCELLED -> {
                                _isGenerating.value = false
                                syncLatestMessages(repo, sessionId)
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("LatexChatViewModel", "Error observing worker: ${e.message}")
            }
        }
    }

    private suspend fun syncLatestMessages(repo: LatexChatRepository, sessionId: String) {
        if (_currentSession.value?.id == sessionId) {
            _messages.value = repo.getMessages(sessionId)
            _sessions.value = repo.getAllSessions()
        }
    }

    fun recompileLatex(context: Context, messageId: String, newLatex: String) {
        val session = _currentSession.value ?: return
        val msg = _messages.value.find { it.id == messageId } ?: return
        val docTitle = msg.pdfTitle ?: "Latex Document"

        updateMessageInMemory(messageId) {
            it.copy(
                latexCode = newLatex,
                pdfStatus = ChatPdfStatus.COMPILING,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            val service = LatexChatService(context)
            try {
                val compileResult = service.compileLatexToPdf(newLatex, docTitle)
                if (compileResult.isSuccess) {
                    val pdfFile = compileResult.getOrNull()
                    repo.updateMessage(session.id, messageId) {
                        it.copy(
                            latexCode = newLatex,
                            pdfFile = pdfFile,
                            pdfStatus = ChatPdfStatus.READY
                        )
                    }
                    syncLatestMessages(repo, session.id)
                } else {
                    val err = compileResult.exceptionOrNull()?.message ?: "Compilation error"
                    repo.updateMessage(session.id, messageId) {
                        it.copy(
                            latexCode = newLatex,
                            pdfStatus = ChatPdfStatus.ERROR,
                            errorMessage = err
                        )
                    }
                    syncLatestMessages(repo, session.id)
                }
            } catch (e: Exception) {
                repo.updateMessage(session.id, messageId) {
                    it.copy(
                        latexCode = newLatex,
                        pdfStatus = ChatPdfStatus.ERROR,
                        errorMessage = e.message
                    )
                }
                syncLatestMessages(repo, session.id)
            }
        }
    }

    fun retryGeneration(context: Context, botMessageId: String) {
        val session = _currentSession.value ?: return
        val msgList = _messages.value
        val botIdx = msgList.indexOfFirst { it.id == botMessageId }
        if (botIdx <= 0) return

        val priorUserMsg = msgList[botIdx - 1]
        if (!priorUserMsg.isUser) return

        viewModelScope.launch(Dispatchers.Main) {
            val repo = repository ?: LatexChatRepository(context).also { repository = it }
            val updated = msgList.filter { it.id != botMessageId }
            _messages.value = updated
            repo.saveMessages(session.id, updated)
            sendMessage(context, priorUserMsg.text)
        }
    }

    fun startEditingMessage(message: ChatMessage) {
        _editingMessage.value = message
    }

    fun stopEditingMessage() {
        _editingMessage.value = null
    }

    suspend fun exportCurrentChatMarkdown(context: Context): String {
        val repo = repository ?: LatexChatRepository(context).also { repository = it }
        val session = _currentSession.value ?: return ""
        return repo.exportSessionAsMarkdown(session.id)
    }

    private fun updateMessageInMemory(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) transform(msg) else msg
        }
    }

    private fun suggestSessionTitle(prompt: String): String {
        val firstLine = prompt.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "New Chat"
        val clean = firstLine.replace(Regex("[^a-zA-Z0-9 _-]"), " ").trim()
        return if (clean.length > 5) clean.take(30) else "New Chat"
    }

    private fun botMsgIdClean(id: String): String = id.replace("-", "").take(12)
}
