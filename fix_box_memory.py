import os

rust_file = "app/src/main/rust/tectonic-jni/src/lib.rs"

if os.path.exists(rust_file):
    with open(rust_file, "r") as f:
        content = f.read()

    # The exact string causing the stack vs heap mismatch
    old_str = "tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path))"
    new_str = "Box::new(tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path)))"

    if old_str in content:
        content = content.replace(old_str, new_str)
        with open(rust_file, "w") as f:
            f.write(content)
        print("✅ SURGICAL LINT REPAIR: Wrapped DirBundle in Box::new() to satisfy heap allocation.")
    else:
        print("⚠️ Could not find the exact string. Maybe it is already boxed?")
else:
    print("❌ lib.rs not found!")
