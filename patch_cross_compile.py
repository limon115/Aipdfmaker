import glob

workflow_files = glob.glob(".github/workflows/*.yml")
if workflow_files:
    filepath = workflow_files[0]
    with open(filepath, 'r') as f:
        yaml_content = f.read()

    # The exact step to replace
    old_step = """      - name: Build Tectonic JNI (Rust)
        run: |
          cd app/src/main/rust/tectonic-jni"""
    
    # The new step with the environment variable injected
    new_step = """      - name: Build Tectonic JNI (Rust)
        env:
          PKG_CONFIG_ALLOW_CROSS: "1"
        run: |
          cd app/src/main/rust/tectonic-jni"""

    if "PKG_CONFIG_ALLOW_CROSS" not in yaml_content:
        yaml_content = yaml_content.replace(old_step, new_step)
        with open(filepath, 'w') as f:
            f.write(yaml_content)
        print(f"✅ {filepath} patched: Cross-compilation bypass injected perfectly!")
    else:
        print("⚡ Bypass already exists.")
else:
    print("❌ No YAML files found in .github/workflows/")
