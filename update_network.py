with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "r") as f:
    text = f.read()

import re

# Update GeminiGenConfig
target = "@Serializable data class GeminiGenConfig(val temperature: Float, val responseMimeType: String? = null)"
replacement = "import kotlinx.serialization.json.JsonObject\n@Serializable data class GeminiGenConfig(val temperature: Float, val responseMimeType: String? = null, val responseSchema: JsonObject? = null)"
text = text.replace(target, replacement)

# Update GeminiRequest creation
target_req = "generationConfig = GeminiGenConfig(temperature, mimeType)"
replacement_req = "generationConfig = GeminiGenConfig(temperature, mimeType, schema)"
text = text.replace(target_req, replacement_req)

# Update sendGeminiRequest signature
target_sig = "private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null): String"
replacement_sig = "private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null, schema: JsonObject? = null): String"
text = text.replace(target_sig, replacement_sig)

# Add JSON Schema definitions
schema_defs = """
    private val blueprintSchema = kotlinx.serialization.json.buildJsonObject {
        put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
        put("properties", kotlinx.serialization.json.buildJsonObject {
            put("courseName", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("chapterName", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("topics", kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                put("items", kotlinx.serialization.json.buildJsonObject {
                    put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                    put("properties", kotlinx.serialization.json.buildJsonObject {
                        put("title", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("durationMinutes", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
                    })
                })
            })
            put("formulaCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("definitionCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("exampleCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("diagramCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("examTipCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
        })
        put("required", kotlinx.serialization.json.buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("courseName"))
            add(kotlinx.serialization.json.JsonPrimitive("chapterName"))
            add(kotlinx.serialization.json.JsonPrimitive("topics"))
            add(kotlinx.serialization.json.JsonPrimitive("formulaCount"))
            add(kotlinx.serialization.json.JsonPrimitive("definitionCount"))
            add(kotlinx.serialization.json.JsonPrimitive("exampleCount"))
            add(kotlinx.serialization.json.JsonPrimitive("diagramCount"))
            add(kotlinx.serialization.json.JsonPrimitive("examTipCount"))
        })
    }

    private val documentSchema = kotlinx.serialization.json.buildJsonObject {
        put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
        put("properties", kotlinx.serialization.json.buildJsonObject {
            put("schemaVersion", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("title", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("author", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("language", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("blocks", kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                put("items", kotlinx.serialization.json.buildJsonObject {
                    put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                    put("properties", kotlinx.serialization.json.buildJsonObject {
                        put("type", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("level", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
                        put("text", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("latex", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("display", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("BOOLEAN")) })
                        put("items", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                            })
                        })
                        put("path", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("columns", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                            })
                        })
                        put("rows", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                                put("items", kotlinx.serialization.json.buildJsonObject {
                                    put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                                })
                            })
                        })
                        put("content", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                                put("properties", kotlinx.serialization.json.buildJsonObject {
                                    put("type", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                    put("value", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                    put("latex", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                })
                            })
                        })
                    })
                    put("required", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("type"))
                    })
                })
            })
        })
        put("required", kotlinx.serialization.json.buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("schemaVersion"))
            add(kotlinx.serialization.json.JsonPrimitive("title"))
            add(kotlinx.serialization.json.JsonPrimitive("blocks"))
        })
    }
"""

text = text.replace("    private val systemPrompt = \"\"\"", schema_defs + "\n    private val systemPrompt = \"\"\"")

# Update generateWithGemini calls
text = text.replace(
    "return sendGeminiRequest(cleanKey, extractedText, systemPrompt, \"application/json\")",
    "return sendGeminiRequest(cleanKey, extractedText, systemPrompt, \"application/json\", blueprintSchema)"
)

# generateContent needs to handle schema
target_gen_content = "suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null): String {"
replacement_gen_content = "suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null, useDocumentSchema: Boolean = false): String {"
text = text.replace(target_gen_content, replacement_gen_content)

target_gen_content2 = "return sendGeminiRequest(cleanKey, prompt, customSystemPrompt, mimeType)"
replacement_gen_content2 = "val schema = if (useDocumentSchema) documentSchema else null\n            return sendGeminiRequest(cleanKey, prompt, customSystemPrompt, mimeType, schema)"
text = text.replace(target_gen_content2, replacement_gen_content2)


with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "w") as f:
    f.write(text)
