import os

j2h_path = "app/src/main/java/com/example/domain/services/html/JsonToHtmlConverter.kt"
with open(j2h_path, "r") as f:
    text = f.read()

if "import timber.log.Timber" not in text:
    text = text.replace('import com.example.domain.models.document.*', 'import com.example.domain.models.document.*\nimport timber.log.Timber')

text = text.replace(
    'fun convert(document: Document): String {',
    'fun convert(document: Document): String {\n        Timber.d("JsonToHtmlConverter: Starting conversion for Document. Blocks count: %d", document.blocks.size)'
)

text = text.replace(
    'return htmlBuilder.toString()',
    'Timber.d("JsonToHtmlConverter: Conversion complete")\n        return htmlBuilder.toString()'
)

with open(j2h_path, "w") as f:
    f.write(text)
