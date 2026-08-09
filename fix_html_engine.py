import sys

with open('app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt', 'r') as f:
    content = f.read()

target_css = """                body {
                    font-family: 'Times New Roman', serif;
                    line-height: 1.6;
                    margin: 40px auto;
                    max-width: 800px;
                    color: #333;
                    padding: 0 20px;
                }
                h1, h2, h3, h4 {
                    color: #2c3e50;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                }
                p {
                    margin-bottom: 1em;
                }
                ul, ol {
                    margin-bottom: 1em;
                }"""

replacement_css = """                @page {
                    size: A4;
                    margin: 0;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    font-family: 'Times New Roman', serif;
                    line-height: 1.6;
                    color: #333;
                }
                body {
                    padding: 40px;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                h1, h2, h3 {
                    color: #2c3e50;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                    break-after: avoid;
                    page-break-after: avoid;
                }
                p, li {
                    margin-bottom: 1em;
                    break-inside: avoid;
                    page-break-inside: avoid;
                }
                .page-break {
                    break-before: page;
                    page-break-before: always;
                }"""

if target_css in content:
    content = content.replace(target_css, replacement_css)
    with open('app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt', 'w') as f:
        f.write(content)
    print("HtmlMergeEngine updated successfully")
else:
    print("CSS Target not found in HtmlMergeEngine")
