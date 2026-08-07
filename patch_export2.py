import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace(
    'view.postDelayed({ finishExport() }, 400)',
    'Handler(Looper.getMainLooper()).postDelayed({ finishExport() }, 400)'
)

with open(path, 'w') as f:
    f.write(content)
