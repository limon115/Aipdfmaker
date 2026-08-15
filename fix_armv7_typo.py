import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # The exact lines we need to patch
    old_cc = 'export CC=$TOOLCHAIN/bin/${arch}${API}-clang'
    old_cxx = 'export CXX=$TOOLCHAIN/bin/${arch}${API}-clang++'

    new_cc = '''clang_prefix=$arch
            if [ "$arch" = "armv7-linux-androideabi" ]; then
                clang_prefix="armv7a-linux-androideabi"
            fi
            export CC=$TOOLCHAIN/bin/${clang_prefix}${API}-clang'''
            
    new_cxx = 'export CXX=$TOOLCHAIN/bin/${clang_prefix}${API}-clang++'

    if old_cc in content:
        content = content.replace(old_cc, new_cc)
        content = content.replace(old_cxx, new_cxx)

        with open(file_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Injected the missing 'a' for the armv7 NDK compiler!")
    else:
        print("⚠️ Could not find the exact CC export line. Check if already patched.")
else:
    print("❌ Could not find build.yml")
