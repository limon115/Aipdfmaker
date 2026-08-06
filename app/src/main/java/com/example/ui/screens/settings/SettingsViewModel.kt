package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AiSettings
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.models.AiProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AiSettingsDataStore(application)

    val settings: StateFlow<AiSettings> = dataStore.aiSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiSettings()
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
}
