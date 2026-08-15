import os

rust_file = "app/src/main/rust/tectonic-jni/src/lib.rs"

if os.path.exists(rust_file):
    with open(rust_file, "r") as f:
        content = f.read()

    # The exact injection to blindfold Fontconfig before the builder starts
    blindfold_code = """    // THE FIX: Blindfold Fontconfig to prevent C++ panic on Android
    std::env::set_var("FONTCONFIG_FILE", "/dev/null");
    std::env::set_var("FONTCONFIG_PATH", "/dev/null");
    
    let mut builder = ProcessingSessionBuilder::default();"""

    if "FONTCONFIG_FILE" not in content:
        content = content.replace(
            "let mut builder = ProcessingSessionBuilder::default();", 
            blindfold_code
        )
        with open(rust_file, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Fontconfig sandbox bypassed. XeTeX will now rely on local font paths!")
    else:
        print("⚠️ Fontconfig blindfold already exists in lib.rs!")
else:
    print("❌ lib.rs not found!")
