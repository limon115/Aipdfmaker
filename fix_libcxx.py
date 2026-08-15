import os

workflow_path = ".github/workflows/build.yml"

if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()

    old_cmd = "cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../../jniLibs build --release"
    
    # We use 'find' to dynamically locate the exact standard libraries and copy them to our output folder
    new_cmd = """cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../../jniLibs build --release

          # THE FIX: Manually package the Android C++ Standard Library so the device can boot Tectonic
          echo "Packaging libc++_shared.so for all architectures..."
          export NDK=${ANDROID_NDK_HOME:-${ANDROID_NDK_LATEST_HOME}}
          find $NDK -name "libc++_shared.so" | grep "aarch64" | head -n 1 | xargs -I {} cp {} ../../jniLibs/arm64-v8a/
          find $NDK -name "libc++_shared.so" | grep "arm-linux-androideabi" | head -n 1 | xargs -I {} cp {} ../../jniLibs/armeabi-v7a/
          find $NDK -name "libc++_shared.so" | grep "x86_64" | head -n 1 | xargs -I {} cp {} ../../jniLibs/x86_64/
          find $NDK -name "libc++_shared.so" | grep "i686" | head -n 1 | xargs -I {} cp {} ../../jniLibs/x86/"""

    if old_cmd in content:
        content = content.replace(old_cmd, new_cmd)
        with open(workflow_path, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Added libc++_shared.so packaging commands to workflow!")
    else:
        print("⚠️ Could not find the target cargo-ndk line. Has it already been modified?")
else:
    print("❌ build.yml not found.")
