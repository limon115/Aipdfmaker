import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    service = f.read()

old_gen = """        val cachedResponse = cache.get(prompt, systemPrompt, ai2Model)
        if (cachedResponse != null) {
            return cleanHtml(cachedResponse)
        }

        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)"""

new_gen = """        val cachedResponse = cache.get(prompt, systemPrompt, ai2Model)
        if (cachedResponse != null) {
            com.example.domain.services.ai.AiUsageTracker.trackCacheHit()
            return cleanHtml(cachedResponse)
        }
        
        com.example.domain.services.ai.AiUsageTracker.trackRequest((prompt.length + systemPrompt.length) / 4)
        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)"""

service = service.replace(old_gen, new_gen)
with open('/app/applet/app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(service)

print("Injected into NoteGenerationService")
