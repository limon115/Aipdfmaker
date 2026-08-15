import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Guard check: only replace if the backslash is NOT already there
    if "\\${prefix}" not in content:
        content = content.replace('${prefix}', '\\${prefix}')
        content = content.replace('${exec_prefix}', '\\${exec_prefix}')
        content = content.replace('${libdir}', '\\${libdir}')
        content = content.replace('${includedir}', '\\${includedir}')

        with open(file_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Escaped pkg-config variables with a single backslash!")
    else:
        print("⚠️ Guard triggered: Variables are already escaped. No extra slashes added.")
else:
    print("❌ Could not find build.yml")
