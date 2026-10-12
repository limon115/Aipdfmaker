package com.example.domain.services.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.utils.AppLogger

/**
 * BroadcastReceiver triggered by the Android OS immediately after this application
 * has been updated / replaced (ACTION_MY_PACKAGE_REPLACED).
 * Automatically cleans up the downloaded APK and update artifacts from storage.
 */
class UpdatePackageReplacedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UpdatePackageReplacedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AppLogger.i(TAG, "Application package updated. Automatically cleaning up update files from storage.")
            ApkInstallerService.cleanupUpdatesDir(context)
        }
    }
}
