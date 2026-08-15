import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Replace the generic dummy name with your actual package name
    if "com.dummy.app" in content:
        content = content.replace("com.dummy.app", "com.aistudio.docmorph.kqbzpa")
        with open(file_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Matched the dummy google-services.json to your exact package name!")
    else:
        print("⚠️ Could not find 'com.dummy.app'. Check if it was already updated.")
else:
    print("❌ Could not find build.yml")
