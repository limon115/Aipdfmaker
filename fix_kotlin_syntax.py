import os

path = "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt"

if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()

    # Safely swap the brackets and braces for both fonts
    content = content.replace(
        r"\setmainfont{Baskervville.ttf}[Path=./]", 
        r"\setmainfont[Path=./]{Baskervville.ttf}"
    )
    content = content.replace(
        r"\newfontfamily\bengalifont{kalpurush.ttf}[Path=./]", 
        r"\newfontfamily\bengalifont[Path=./]{kalpurush.ttf}"
    )

    with open(path, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Fixed LaTeX font syntax ordering!")
else:
    print("❌ Could not find LatexCompilerRepository.kt")
