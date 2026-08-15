import os

workflow_path = ".github/workflows/build.yml"

if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()

    # The exact command that just copies the archive
    old_step = "cp vendor_tars/tlextras-2021.3r1.tar app/src/main/assets/tectonic-bundle/default.ttb"
    
    # The fix: Create the directory and fully extract the tarball into it
    new_step = "mkdir -p app/src/main/assets/tectonic-bundle/default.ttb && tar -xf vendor_tars/tlextras-2021.3r1.tar -C app/src/main/assets/tectonic-bundle/default.ttb"

    if old_step in content:
        content = content.replace(old_step, new_step)
        with open(workflow_path, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Replaced 'cp' with 'tar -xf'. DirBundle will now have a real directory!")
    else:
        print("⚠️ Could not find the copy command. Already patched?")
else:
    print("❌ build.yml not found.")
