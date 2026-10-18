package com.example.ui.screens.blueprint

import androidx.lifecycle.ViewModel
import com.example.domain.models.BlueprintSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlueprintViewModel : ViewModel() {
    private val _state = MutableStateFlow<BlueprintSummary?>(null)
    val state: StateFlow<BlueprintSummary?> = _state.asStateFlow()
    
    fun setBlueprintSummary(summary: BlueprintSummary) {
        _state.value = summary
    }

    fun deleteTopicAt(index: Int) {
        val current = _state.value ?: return
        if (index in current.topics.indices) {
            val updatedTopics = current.topics.toMutableList().apply { removeAt(index) }
            _state.value = current.copy(topics = updatedTopics)
        }
    }

    fun deleteTopic(topic: com.example.domain.models.Topic) {
        val current = _state.value ?: return
        val updatedTopics = current.topics.filter { it != topic }
        _state.value = current.copy(topics = updatedTopics)
    }

    fun addTopic(title: String, durationMinutes: Int = 20) {
        val current = _state.value ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isNotBlank()) {
            val newTopic = com.example.domain.models.Topic(title = cleanTitle, durationMinutes = durationMinutes)
            _state.value = current.copy(topics = current.topics + newTopic)
        }
    }
}
