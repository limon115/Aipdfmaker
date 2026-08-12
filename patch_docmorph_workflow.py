import os
import glob

workflow_files = glob.glob(".github/workflows/*.yml")
if workflow_files:
    filepath = workflow_files[0]
    with open(filepath, 'r') as f:
        yaml_content = f.read()

    old_step = "      - name: Generate Gradle Wrapper (Cloud Execution)"
    
    new_steps = """      - name: Install Rust & cargo-ndk
        run: |
          rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
          cargo install cargo-ndk

      - name: Build Tectonic JNI (Rust)
        run: |
          cd app/src/main/rust/tectonic-jni
          cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../../../libs build --release

      - name: Generate Gradle Wrapper (Cloud Execution)"""

    if "Install Rust & cargo-ndk" not in yaml_content:
        yaml_content = yaml_content.replace(old_step, new_steps)
        with open(filepath, 'w') as f:
            f.write(yaml_content)
        print(f"✅ {filepath} patched: Rust & cargo-ndk steps perfectly injected into DocMorph workflow!")
    else:
        print("⚡ Rust steps already exist in workflow.")
else:
    print("❌ No YAML files found in .github/workflows/")
