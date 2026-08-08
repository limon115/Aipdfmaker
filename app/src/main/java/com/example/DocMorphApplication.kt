package com.example

import android.app.Application
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.datastore.AiModelConfigCache
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DocMorphApplication : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Eagerly initialize DataStore caching mechanism to eliminate startup delays
        GlobalScope.launch {
            val dataStore = AiSettingsDataStore(this@DocMorphApplication)
            dataStore.aiSettingsFlow.first()
            
            val modelCache = AiModelConfigCache(this@DocMorphApplication)
            modelCache.modelMetadataFlow.first()
        }
    }
}
