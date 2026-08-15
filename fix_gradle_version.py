import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Replace the outdated hardcoded version with the required 9.3.1
    if "8.10.2" in content:
        content = content.replace("8.10.2", "9.3.1")
        with open(file_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Upgraded Gradle from 8.10.2 to 9.3.1 to satisfy AGP 9.1.1!")
    else:
        print("⚠️ Could not find 8.10.2 in build.yml. It may already be updated.")
else:
    print("❌ Could not find build.yml")
