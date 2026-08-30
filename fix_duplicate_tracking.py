import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)\n        com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)',
    'com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)'
)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
