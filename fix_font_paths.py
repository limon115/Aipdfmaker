import os

path = "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt"

if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()

    # Replace the relative Unix paths with dynamic Kotlin absolute paths
    content = content.replace(
        r"\setmainfont[Path=./]{Baskervville.ttf}", 
        r"\setmainfont[Path=${baseDir.absolutePath}/]{Baskervville.ttf}"
    )
    content = content.replace(
        r"\newfontfamily\bengalifont[Path=./]{kalpurush.ttf}", 
        r"\newfontfamily\bengalifont[Path=${baseDir.absolutePath}/]{kalpurush.ttf}"
    )

    with open(path, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Dynamic absolute paths injected into LaTeX template! XeTeX will now find the fonts.")
else:
    print("❌ Could not find LatexCompilerRepository.kt")
