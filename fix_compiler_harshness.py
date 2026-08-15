import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    target = 'export RANLIB=$TOOLCHAIN/bin/llvm-ranlib'
    replacement = 'export RANLIB=$TOOLCHAIN/bin/llvm-ranlib\n            export CFLAGS="-Wno-error -fPIC"\n            export CXXFLAGS="-Wno-error -fPIC"'

    # Guard check to ensure we don't duplicate it
    if 'export CFLAGS="-Wno-error' not in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Injected -Wno-error to force the NDK compiler to stop being so harsh on zlib!")
    else:
        print("⚠️ Guard triggered: CFLAGS already patched.")
else:
    print("❌ Could not find build.yml")
