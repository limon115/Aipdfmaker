import sys

with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

# Replace HttpRequestRetry block
old_retry = """        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            retryIf { request, response ->
                response.status.value == 429
            }
            exponentialDelay()
        }"""

new_retry = """        install(HttpRequestRetry) {
            maxRetries = 5
            retryIf { request, response ->
                response.status.value == 429 || response.status.value >= 500
            }
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            delayMillis { retry ->
                // Calculate exponential backoff with jitter (10s, 20s, 40s, 80s...)
                val baseDelay = (10000L * Math.pow(2.0, (retry - 1).toDouble())).toLong()
                val jitter = (Math.random() * 2000).toLong() // 0-2s jitter
                
                // If the server provides a Retry-After header, respect it
                var retryAfterMs = 0L
                try {
                    val retryAfterStr = response?.headers?.get(io.ktor.http.HttpHeaders.RetryAfter)
                    if (retryAfterStr != null) {
                        retryAfterMs = retryAfterStr.toLongOrNull()?.times(1000L) ?: 0L
                    }
                } catch(e: Exception) {}
                
                if (retryAfterMs > 0) retryAfterMs + jitter else baseDelay + jitter
            }
        }"""

if old_retry in content:
    content = content.replace(old_retry, new_retry)

with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

print("Updated AiNetworkClient")
