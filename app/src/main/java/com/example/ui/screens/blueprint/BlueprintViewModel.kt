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
}
