import os

files_to_patch = [
    "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt",
    "app/src/main/java/com/example/domain/services/export/ExportEngine.kt"
]

for filepath in files_to_patch:
    if not os.path.exists(filepath):
        print(f"⚠️ Skipped: '{filepath}' not found. Are you in the project root?")
        continue

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    content_str = "".join(lines)
    if "AutoFakeBold" in content_str and "Devanagari" in content_str:
        print(f"⏭️ Already patched: {filepath}")
        continue

    with open(filepath, 'w', encoding='utf-8') as f:
        skip_lines = 0
        for i, line in enumerate(lines):
            if skip_lines > 0:
                skip_lines -= 1
                continue
            
            # FIX 1: Inject AutoFakeBold and AutoFakeSlant gracefully
            if "Language=Bengali" in line:
                indent = line.split("Language=")[0]
                f.write(indent + "Language=Bengali,\n")
                f.write(indent + "AutoFakeBold=1.5,\n")
                f.write(indent + "AutoFakeSlant=0.2\n")
                continue
            
            # FIX 2 & 3: Replace \begingroup transitions with safe, Devanagari-inclusive mapping
            if "\\setTransitionsFor{Bengali}" in line and i + 1 < len(lines) and "\\begingroup" in lines[i+1]:
                indent = line.split("\\setTransitionsFor")[0]
                f.write(indent + "\\setTransitionsFor{Bengali}{\\bengalifont}{}\n")
                f.write(indent + "\\setTransitionsFor{Devanagari}{\\bengalifont}{}\n")
                f.write(indent + "\\setTransitionsFor{BasicLatin}{\\rmfamily}{}\n")
                skip_lines = 2 # Skip the old {\begingroup...} and {\endgroup} lines
                continue

            f.write(line)
        
    print(f"✅ Successfully patched: {filepath}")

print("🚀 All Kotlin templates surgically updated with the correct preamble!")
