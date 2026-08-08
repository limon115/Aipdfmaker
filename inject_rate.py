import sys

with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    client = f.read()

old_retry = """            retryIf { request, response ->
                response.status.value == 429 || response.status.value >= 500
            }"""

new_retry = """            retryIf { request, response ->
                if (response.status.value == 429) {
                    com.example.domain.services.ai.AiUsageTracker.trackRateLimitError()
                }
                response.status.value == 429 || response.status.value >= 500
            }"""

client = client.replace(old_retry, new_retry)
with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(client)

print("Injected")
