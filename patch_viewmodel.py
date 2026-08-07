import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '                if (e is ClientRequestException) {\n                    onError("API Error " + e.response.status.value)\n                } else if (e is HttpRequestTimeoutException) {\n                    onError("Request timed out")\n                } else {\n                    onError("NET: " + (e.message ?: "").takeLast(35))\n                }',
    '                if (e is ClientRequestException) {\n                    onError("API Error " + e.response.status.value + ": " + e.message)\n                } else if (e is HttpRequestTimeoutException) {\n                    onError("Request timed out: " + e.message)\n                } else {\n                    onError("NET Error: " + (e.message ?: "Unknown"))\n                }'
)

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'w') as f:
    f.write(content)
