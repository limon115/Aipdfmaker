import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

# For Gemini
content = content.replace(
    'val targetModel = model.ifBlank { "gemini-1.5-flash" }',
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(estimatedTokens)\n        val targetModel = model.ifBlank { "gemini-1.5-flash" }'
)

# For OpenAI
content = content.replace(
    'val requestPayload = OpenAiRequest(reqModel, messages, temp, maxTokens)',
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(estimatedTokens)\n        val requestPayload = OpenAiRequest(reqModel, messages, temp, maxTokens)'
)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    notes_content = f.read()

notes_content = re.sub(r'com\.example\.domain\.services\.ai\.AiUsageTracker\.trackRequest\(\(prompt\.length \+ systemPrompt\.length\) / 4\)\n\s*', '', notes_content)

with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(notes_content)
