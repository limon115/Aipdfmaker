import re

# 1. Update AiNetworkClient constructor
with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'class AiNetworkClient(private val provider: String, private val apiKey: String, private val model: String, private val temperature: Float) {',
    'class AiNetworkClient(private val provider: String, private val apiKey: String, private val model: String, private val temperature: Float, private val featureName: String = "AI 1 - Blueprint") {'
)

# 2. Update trackRequest calls
content = content.replace(
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(estimatedTokens)',
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)'
)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

# 3. NoteGenerationService/Worker
with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    ngs_content = f.read()
ngs_content = ngs_content.replace(
    'val clientForGeneration = AiNetworkClient(ai2Provider, ai2ApiKey, ai2Model, ai2Temperature)',
    'val clientForGeneration = AiNetworkClient(ai2Provider, ai2ApiKey, ai2Model, ai2Temperature, "AI 2 - Generator")'
)
with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(ngs_content)

with open('app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    ngw_content = f.read()
ngw_content = ngw_content.replace(
    'val dummyClient = AiNetworkClient(settings.ai2Provider.name, settings.ai2ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }, settings.ai2Model.ifBlank { "gemini-1.5-flash" }, settings.ai2Temperature)',
    'val dummyClient = AiNetworkClient(settings.ai2Provider.name, settings.ai2ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }, settings.ai2Model.ifBlank { "gemini-1.5-flash" }, settings.ai2Temperature, "AI 2 - Generator")'
)
with open('app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(ngw_content)

# 4. BlueprintWorker
with open('app/src/main/java/com/example/domain/services/worker/BlueprintWorker.kt', 'r') as f:
    bw_content = f.read()
bw_content = bw_content.replace(
    'val aiClient = AiNetworkClient(settings.ai1Provider.name, settings.ai1ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }, settings.ai1Model.ifBlank { "gemini-1.5-flash" }, 0.7f)',
    'val aiClient = AiNetworkClient(settings.ai1Provider.name, settings.ai1ApiKey.ifBlank { com.example.BuildConfig.GEMINI_API_KEY }, settings.ai1Model.ifBlank { "gemini-1.5-flash" }, 0.7f, "AI 1 - Blueprint")'
)
with open('app/src/main/java/com/example/domain/services/worker/BlueprintWorker.kt', 'w') as f:
    f.write(bw_content)

# 5. LatexDebuggerViewModel
with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerViewModel.kt', 'r') as f:
    ld_content = f.read()
ld_content = ld_content.replace(
    'val networkClient = AiNetworkClient(provider, apiKey, model, 0.7f)',
    'val networkClient = AiNetworkClient(provider, apiKey, model, 0.7f, "AI 3 - Debugger")'
)
with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerViewModel.kt', 'w') as f:
    f.write(ld_content)

# 6. SettingsViewModel
with open('app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'r') as f:
    svm_content = f.read()
svm_content = svm_content.replace(
    'val client = AiNetworkClient(provider, apiKey, model, 0.7f)',
    'val client = AiNetworkClient(provider, apiKey, model, 0.7f, "Settings Test")'
)
with open('app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'w') as f:
    f.write(svm_content)
