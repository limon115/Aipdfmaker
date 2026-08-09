package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import kotlinx.serialization.json.Json
import com.example.domain.models.document.Document

class ExportEngine(private val context: Context) {
    fun exportProjectFiles(
        projectName: String,
        jsonContent: String,
        onComplete: (pdfFile: File?, jsonFile: File) -> Unit
    ) {
        try {
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")
            
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
            }
            baseDir.mkdirs()
            
            val jsonFile = File(baseDir, "document.json")
            jsonFile.writeText(jsonContent)
            
            val pdfFile = File(baseDir, "document.pdf")
            
            try {
                // Ignore unknown keys so it doesn't crash on slightly malformed AI output
                val jsonFormat = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
                val document = jsonFormat.decodeFromString<Document>(jsonContent)
                
                val engine = NativePdfEngine(context)
                engine.exportDocumentToPdf(document, pdfFile)
                
                onComplete(pdfFile, jsonFile)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onComplete(null, jsonFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
