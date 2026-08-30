import re
import os

with open('app/src/main/java/com/example/domain/services/pdf/TermuxXeLaTeXBridge.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'suspend fun compile(context: Context, texFile: File): Result<File>',
    'suspend fun compile(context: Context, texFile: File, fixScript: String? = null): Result<File>'
)

content = content.replace(
    'val jsonBody = JSONObject().apply { put("latex", latexContent) }.toString()',
    'val jsonBody = JSONObject().apply { put("latex", latexContent); fixScript?.let { put("fix_script", it) } }.toString()'
)

with open('app/src/main/java/com/example/domain/services/pdf/TermuxXeLaTeXBridge.kt', 'w') as f:
    f.write(content)
