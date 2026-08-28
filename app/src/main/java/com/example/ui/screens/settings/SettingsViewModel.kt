package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AiSettings
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.models.AiProvider
import com.example.domain.models.ThemeMode
import com.example.data.network.AiNetworkClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AiSettingsDataStore(application)

    val settings: StateFlow<AiSettings?> = dataStore.aiSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateAi1Provider(provider: AiProvider) {
        viewModelScope.launch { dataStore.updateAi1Provider(provider) }
    }

    fun updateAi1Model(model: String) {
        viewModelScope.launch { dataStore.updateAi1Model(model) }
    }

    fun updateAi1ApiKey(apiKey: String) {
        viewModelScope.launch { dataStore.updateAi1ApiKey(apiKey) }
    }

    fun updateAi2Provider(provider: AiProvider) {
        viewModelScope.launch { dataStore.updateAi2Provider(provider) }
    }

    fun updateAi2Model(model: String) {
        viewModelScope.launch { dataStore.updateAi2Model(model) }
    }

    fun updateAi2ApiKey(apiKey: String) {
        viewModelScope.launch { dataStore.updateAi2ApiKey(apiKey) }
    }

    fun updateAi2Advanced(temperature: Float, maxTokens: Int, topP: Float) {
        viewModelScope.launch { dataStore.updateAi2Advanced(temperature, maxTokens, topP) }
    }


    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { dataStore.updateThemeMode(themeMode) }
    }

    fun saveCustomFont(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val fontFile = java.io.File(context.filesDir, "custom_ui_font.ttf")
                inputStream.use { input ->
                    fontFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                dataStore.updateCustomFontPath(fontFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearCustomFont() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fontFile = java.io.File(context.filesDir, "custom_ui_font.ttf")
                if (fontFile.exists()) fontFile.delete()
                dataStore.updateCustomFontPath("")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun testConnection(
        provider: String,
        model: String,
        apiKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanKey = apiKey.replace(" ", "")
        val lowerProvider = provider.lowercase()
        
        if (cleanKey.isEmpty() && !lowerProvider.contains("ollama") && !lowerProvider.contains("lm studio")) {
            onError("Please enter an API Key first.")
            return
        }

        viewModelScope.launch {
            try {
                val client = AiNetworkClient(
                    provider = provider,
                    apiKey = cleanKey,
                    model = model,
                    temperature = 0.7f
                )
                
                val isSuccess = client.testConnection()
                if (isSuccess) {
                    onSuccess()
                } else {
                    onError("Unknown error")
                }
            } catch (e: Throwable) {
                if (e is ClientRequestException) {
                    onError("API Error " + e.response.status.value + ": " + e.message)
                } else if (e is HttpRequestTimeoutException) {
                    onError("Request timed out: " + e.message)
                } else {
                    onError("NET Error: " + (e.message ?: "Unknown"))
                }
            }
        }
    }
}
