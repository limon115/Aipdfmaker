import sys

with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()
print("Contains HttpRequestRetry: ", "HttpRequestRetry" in content)
