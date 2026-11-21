package com.example

import android.app.Application
import timber.log.Timber
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.datastore.AiModelConfigCache
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DocMorphApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // Clean up leftover update files from storage
        try {
            com.example.domain.services.updater.ApkInstallerService.cleanupUpdatesDir(this)
        } catch (e: Exception) {
            Timber.w(e, "Failed to clean up update files on app startup")
        }

        // Eagerly initialize DataStore caching using process lifecycle
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val dataStore = AiSettingsDataStore(this@DocMorphApplication)
            dataStore.aiSettingsFlow.first()

            val modelCache = AiModelConfigCache(this@DocMorphApplication)
            modelCache.modelMetadataFlow.first()
        }
    }
}
