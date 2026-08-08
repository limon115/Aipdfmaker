import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace(
    'webView.settings.javaScriptEnabled = false',
    'webView.settings.javaScriptEnabled = false\n                    webView.settings.textZoom = 100\n                    webView.setInitialScale(100)'
)

with open(path, 'w') as f:
    f.write(content)

path2 = '/app/applet/app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt'
with open(path2, 'r') as f:
    content2 = f.read()

content2 = content2.replace(
    '<meta charset=\\"UTF-8\\">',
    '<meta charset=\\"UTF-8\\"><meta name=\\"viewport\\" content=\\"width=device-width, initial-scale=1.0\\">'
)

with open(path2, 'w') as f:
    f.write(content2)
