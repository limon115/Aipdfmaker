import sys

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('result?.replace(""", "")', 'result?.replace("\\"", "")')

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
    f.write(content)
print("Quotes escaped correctly")
