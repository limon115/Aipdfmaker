import os

workflow_path = ".github/workflows/build.yml"

if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()

    # The exact typo causing the empty APK
    old_path = "-o ../../../jniLibs"
    new_path = "-o ../../jniLibs"

    if old_path in content:
        content = content.replace(old_path, new_path)
        with open(workflow_path, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Fixed directory traversal. Native libs will now land in app/src/main/jniLibs!")
    else:
        print("⚠️ Could not find the bad path. Already fixed?")
else:
    print("❌ build.yml not found.")
