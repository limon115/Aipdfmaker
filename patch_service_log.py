with open("app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt", "r") as f:
    text = f.read()

target = """        try {
            val response = aiNetworkClient.generateContent(
                extractedText = combinedText,"""
replacement = """        try {
            com.example.utils.AppLogger.i("NoteGenService", "Generating notes for topic: ${topic.title} (${combinedText.length} chars)")
            val response = aiNetworkClient.generateContent(
                extractedText = combinedText,"""
if target in text:
    text = text.replace(target, replacement)

target2 = """            } catch (e: Exception) {
                e.printStackTrace()
            }"""
replacement2 = """            } catch (e: Exception) {
                com.example.utils.AppLogger.e("NoteGenService", "Error parsing blocks for topic ${topic.title}", e)
                e.printStackTrace()
            }"""
if target2 in text:
    text = text.replace(target2, replacement2)

target3 = """        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }"""
replacement3 = """        } catch (e: Exception) {
            com.example.utils.AppLogger.e("NoteGenService", "Failed to generate notes for topic ${topic.title}", e)
            e.printStackTrace()
            return emptyList()
        }"""
if target3 in text:
    text = text.replace(target3, replacement3)

with open("app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt", "w") as f:
    f.write(text)

