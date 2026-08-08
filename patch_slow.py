import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace(
    'val webView = WebView(context)',
    'WebView.enableSlowWholeDocumentDraw()\n                    val webView = WebView(context)'
)

with open(path, 'w') as f:
    f.write(content)
