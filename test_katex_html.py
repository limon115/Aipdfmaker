with open("app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt", "r") as f:
    content = f.read()

target = """<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"""
replacement = """<head>
<meta charset=\"UTF-8\">
<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
<!-- KaTeX -->
<link rel=\"stylesheet\" href=\"katex/katex.min.css\">
<script defer src=\"katex/katex.min.js\"></script>
<script defer src=\"katex/contrib/auto-render.min.js\"></script>
<script defer src=\"katex/contrib/mhchem.min.js\"></script>
<script>
    document.addEventListener("DOMContentLoaded", function() {
        renderMathInElement(document.body, {
            delimiters: [
                {left: "$$", right: "$$", display: true},
                {left: "\\[", right: "\\]", display: true},
                {left: "$", right: "$", display: false},
                {left: "\\(", right: "\\)", display: false}
            ],
            throwOnError: false
        });
        document.body.setAttribute('data-math-rendered', 'true');
    });
</script>
</head>"""

content = content.replace(target, replacement)

# We also need to add bengali font support if possible, or just rely on system fonts for Bengali.
css_target = """                html, body {
                    margin: 0;
                    padding: 0;
                    font-family: 'Times New Roman', serif;
                    line-height: 1.6;
                    color: #333;
                }"""
css_replacement = """                html, body {
                    margin: 0;
                    padding: 0;
                    font-family: 'Kalpurush', 'SolaimanLipi', 'Times New Roman', serif;
                    line-height: 1.6;
                    color: #333;
                }
                .math-block {
                    break-inside: avoid;
                    page-break-inside: avoid;
                }"""
content = content.replace(css_target, css_replacement)

with open("app/src/main/java/com/example/domain/services/html/HtmlMergeEngine.kt", "w") as f:
    f.write(content)
