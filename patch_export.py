import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace(
    'webView.handler.postDelayed({ finishExport() }, 2000)',
    'Handler(Looper.getMainLooper()).postDelayed({ finishExport() }, 2000)'
)

with open(path, 'w') as f:
    f.write(content)
