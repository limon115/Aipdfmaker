import sys

path = '/app/applet/app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace(
    'val doc: Document = Jsoup.parse("<html><head></head><body></body></html>")',
    'val doc: Document = Jsoup.parse("<html><head><meta charset=\\"UTF-8\\"></head><body></body></html>")'
)

with open(path, 'w') as f:
    f.write(content)
