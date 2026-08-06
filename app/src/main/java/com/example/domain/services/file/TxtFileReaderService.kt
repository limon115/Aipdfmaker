package com.example.domain.services.file

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TxtFileReaderService {
    suspend fun readTextFromUri(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("Could not open input stream for URI: $uri")
    }
}
