package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.modelConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_model_config")

class AiModelConfigCache(private val context: Context) {
    companion object {
        val MODEL_METADATA = stringPreferencesKey("model_metadata")
    }

    val modelMetadataFlow: Flow<String> = context.modelConfigDataStore.data
        .map { preferences ->
            preferences[MODEL_METADATA] ?: "{}"
        }

    suspend fun saveModelMetadata(metadataJson: String) {
        context.modelConfigDataStore.edit { preferences ->
            preferences[MODEL_METADATA] = metadataJson
        }
    }
}
