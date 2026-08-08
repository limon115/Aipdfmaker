package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.services.ai.GeminiApiTester
import com.example.domain.services.ai.GeminiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TestState {
    object Idle : TestState()
    object Testing : TestState()
    data class Success(val models: List<GeminiModel>, val latencyMs: Long) : TestState()
    data class Error(val code: String, val message: String) : TestState()
}

sealed class ModelTestState {
    object Idle : ModelTestState()
    object Testing : ModelTestState()
    data class Success(val response: String, val latencyMs: Long) : ModelTestState()
    data class Error(val code: String, val message: String) : ModelTestState()
}

class ApiLabViewModel : ViewModel() {
    private val apiTester = GeminiApiTester()

    private val _apiKeyTestState = MutableStateFlow<TestState>(TestState.Idle)
    val apiKeyTestState: StateFlow<TestState> = _apiKeyTestState.asStateFlow()

    private val _modelTestState = MutableStateFlow<ModelTestState>(ModelTestState.Idle)
    val modelTestState: StateFlow<ModelTestState> = _modelTestState.asStateFlow()

    fun testApiKey(apiKey: String) {
        _apiKeyTestState.value = TestState.Testing
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = apiTester.getAvailableModels(apiKey)
            val latency = System.currentTimeMillis() - startTime
            
            result.onSuccess { models ->
                _apiKeyTestState.value = TestState.Success(models, latency)
            }.onFailure { error ->
                _apiKeyTestState.value = parseError(error.message ?: "Unknown Error", "API Key Test Failed")
            }
        }
    }

    fun testModel(apiKey: String, modelName: String, prompt: String) {
        _modelTestState.value = ModelTestState.Testing
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = apiTester.testModel(apiKey, modelName, prompt)
            val latency = System.currentTimeMillis() - startTime
            
            result.onSuccess { text ->
                _modelTestState.value = ModelTestState.Success(text, latency)
            }.onFailure { error ->
                _modelTestState.value = parseModelTestError(error.message ?: "Unknown Error", "Model Test Failed")
            }
        }
    }
    
    fun resetTestState() {
        _apiKeyTestState.value = TestState.Idle
        _modelTestState.value = ModelTestState.Idle
    }
    
    fun resetModelTestState() {
        _modelTestState.value = ModelTestState.Idle
    }

    private fun parseError(errorMessage: String, defaultMessage: String): TestState.Error {
        val messageStr = errorMessage.lowercase()
        return when {
            messageStr.contains("401") -> TestState.Error("401", "The API key was rejected.")
            messageStr.contains("403") -> TestState.Error("403", "The API key cannot access this request.")
            messageStr.contains("429") -> TestState.Error("429", "The request was rejected because a quota or rate limit was reached.")
            messageStr.contains("network") || messageStr.contains("unknownhost") || messageStr.contains("timeout") -> 
                TestState.Error("Network", "No response received from Gemini.")
            else -> TestState.Error("Error", defaultMessage)
        }
    }
    
    private fun parseModelTestError(errorMessage: String, defaultMessage: String): ModelTestState.Error {
        val messageStr = errorMessage.lowercase()
        return when {
            messageStr.contains("401") -> ModelTestState.Error("401", "The API key was rejected.")
            messageStr.contains("403") -> ModelTestState.Error("403", "The API key cannot access this request.")
            messageStr.contains("429") -> ModelTestState.Error("429", "The request was rejected because a quota or rate limit was reached.")
            messageStr.contains("network") || messageStr.contains("unknownhost") || messageStr.contains("timeout") -> 
                ModelTestState.Error("Network", "No response received from Gemini.")
            else -> ModelTestState.Error("Error", defaultMessage)
        }
    }
}
