import os

workflow_path = ".github/workflows/build.yml"

if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()

    # The exact block to find and replace
    old_step = """      - name: Download Tectonic TeX Bundle
        run: |
          mkdir -p app/src/main/assets/tectonic-bundle
          echo "Downloading Tectonic TeX Live bundle directly from working server..."
          curl -fL --retry 5 --retry-delay 5 -o app/src/main/assets/tectonic-bundle/default.ttb "https://data1.fullyjustified.net/tlextras-2021.3r1.tar\""""

    new_step = """      - name: Setup Tectonic TeX Bundle
        run: |
          mkdir -p app/src/main/assets/tectonic-bundle
          echo "Extracting vendored Tectonic TeX Live bundle..."
          cp vendor_tars/tlextras-2021.3r1.tar app/src/main/assets/tectonic-bundle/default.ttb"""

    if old_step in content:
        content = content.replace(old_step, new_step)
        with open(workflow_path, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Workflow updated to use local vendored Tectonic bundle.")
    else:
        print("⚠️ Could not find the curl step. It might have already been patched!")
else:
    print("❌ build.yml not found.")
