import os

path = "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt"

if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()

    # Align the Kotlin output filename with the Rust input expectation
    content = content.replace('File(baseDir, "document.tex")', 'File(baseDir, "main.tex")')

    with open(path, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Kotlin will now generate 'main.tex', matching Tectonic's expectations perfectly.")
else:
    print("❌ Could not find LatexCompilerRepository.kt")
