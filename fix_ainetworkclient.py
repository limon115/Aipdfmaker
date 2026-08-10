import re
with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "r") as f:
    text = f.read()

# insert private val jsonFormat
if "private val jsonFormat = Json { ignoreUnknownKeys = true }" not in text:
    text = text.replace("class AiNetworkClient(", "class AiNetworkClient(\n    val provider: String = \"Gemini\",\n    val apiKey: String = \"\",\n    val model: String = \"gemini-2.5-flash\",\n    val temperature: Float = 0.7f\n) {\n\n    private val jsonFormat = Json { ignoreUnknownKeys = true }\n")

# remove duplicate class constructor args if any were added by mistake, wait I can just use string replace carefully
