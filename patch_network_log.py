with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "r") as f:
    text = f.read()

target = """        try {
            val response: HttpResponse = ktorClient.post(url) {"""
replacement = """        com.example.utils.AppLogger.d("AiNetwork", "Sending Gemini request to $targetModel (${prompt.length} chars)")
        try {
            val response: HttpResponse = ktorClient.post(url) {"""
if target in text:
    text = text.replace(target, replacement)

target2 = """            val rawText = jsonResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from Gemini")"""
replacement2 = """            com.example.utils.AppLogger.d("AiNetwork", "Gemini request successful")
            val rawText = jsonResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from Gemini")"""
if target2 in text:
    text = text.replace(target2, replacement2)

target3 = """        } catch (e: ClientRequestException) {
            throw Exception("API Error ${e.response.status.value}: ${e.response.bodyAsText().take(50)}")
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message?.take(50)}")
        }"""
replacement3 = """        } catch (e: ClientRequestException) {
            val err = "API Error ${e.response.status.value}: ${e.response.bodyAsText().take(50)}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        } catch (e: Exception) {
            val err = "Network Error: ${e.message?.take(50)}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        }"""
if target3 in text:
    text = text.replace(target3, replacement3)

target4 = """        try {
            val response: HttpResponse = ktorClient.post(baseUrl) {"""
replacement4 = """        com.example.utils.AppLogger.d("AiNetwork", "Sending OpenAI request to $reqModel (${messages.size} messages)")
        try {
            val response: HttpResponse = ktorClient.post(baseUrl) {"""
if target4 in text:
    text = text.replace(target4, replacement4)

target5 = """            val rawText = jsonResponse.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from Provider")"""
replacement5 = """            com.example.utils.AppLogger.d("AiNetwork", "OpenAI request successful")
            val rawText = jsonResponse.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from Provider")"""
if target5 in text:
    text = text.replace(target5, replacement5)

target6 = """        } catch (e: ClientRequestException) {
            throw Exception("API Error ${e.response.status.value}")
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message}")
        }"""
replacement6 = """        } catch (e: ClientRequestException) {
            val err = "API Error ${e.response.status.value}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        } catch (e: Exception) {
            val err = "Network Error: ${e.message}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        }"""
if target6 in text:
    text = text.replace(target6, replacement6)

with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "w") as f:
    f.write(text)

