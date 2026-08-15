import os, re

rust_file = "app/src/main/rust/tectonic-jni/src/lib.rs"
if os.path.exists(rust_file):
    with open(rust_file, "r") as f:
        content = f.read()

    # Inject the TECTONIC_CACHE_DIR environment variable so the app can save network packages
    if "TECTONIC_CACHE_DIR" not in content:
        content = content.replace(
            'std::env::set_var("FONTCONFIG_PATH", "/dev/null");',
            'std::env::set_var("FONTCONFIG_PATH", "/dev/null");\n    std::env::set_var("TECTONIC_CACHE_DIR", &output_dir);'
        )

    # Remove the hardcoded .bundle() offline declaration so it defaults to the Network Bundle
    content = re.sub(r"\s*\.bundle\(Box::new\(tectonic_bundles::dir::DirBundle::new[^)]+\)\)\)\n", "\n", content)

    with open(rust_file, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Rust backend switched to dynamic Network Bundle with Android caching.")
