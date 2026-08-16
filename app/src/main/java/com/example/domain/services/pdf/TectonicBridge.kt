package com.example.domain.services.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TectonicBridge {
    init {
        try {
            System.loadLibrary("tectonic_jni")
        } catch (e: UnsatisfiedLinkError) {
            com.example.utils.AppLogger.e("TectonicBridge", "Could not load tectonic_jni library. Have you compiled the Rust crate and placed .so in jniLibs?", e)
        }
    }
    external fun compileToPdf(texSource: String, bundlePath: String, outputDir: String): String

    suspend fun compileLatex(context: Context, tex: String): Result<File> = withContext(kotlinx.coroutines.Dispatchers.Unconfined) {
        runCatching {
            val bundlePath = ensureBundleExtracted(context)
            val outDir = context.cacheDir.absolutePath
            val resultPath = compileToPdf(tex, bundlePath, outDir)
            if (resultPath.startsWith("Error")) {
                throw Exception(resultPath) // 🔴 THE FIX: Throw the exact Rust error message
            }
            File(resultPath)
        }
    }

    private fun ensureBundleExtracted(context: Context): String {
        val bundleDir = File(context.filesDir, "tectonic-bundle")
        if (!bundleDir.exists()) {
            bundleDir.mkdirs()
            try {
                val assets = context.assets.list("tectonic-bundle") ?: emptyArray()
                for (asset in assets) {
                    context.assets.open("tectonic-bundle/$asset").use { input ->
                        File(bundleDir, asset).outputStream().use { output ->
                            // Explicit 16KB chunked buffering prevents OOM crashes
                            // when unpacking massive assets on low-RAM devices
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                com.example.utils.AppLogger.e("TectonicBridge", "Error extracting tectonic bundle", e)
            }
        }
        return bundleDir.absolutePath
    }
}
