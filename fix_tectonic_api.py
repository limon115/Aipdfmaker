import os

rust_file = "app/src/main/rust/tectonic-jni/src/lib.rs"
cargo_toml = "app/src/main/rust/tectonic-jni/Cargo.toml"

print("🔍 Initiating surgical lint repairs for Tectonic V2 API...")

# 1. Update Rust implementation
with open(rust_file, "r") as f:
    content = f.read()

# Fix Bundle API change (Moved to tectonic_bundles crate)
content = content.replace(
    "tectonic::io::local_dir_bundle::LocalDirBundle::new(&bundle_path).unwrap()",
    "tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path))"
)

# Fix Session Builder API change (build() renamed to create() with dropped arg)
content = content.replace(
    ".build(None, &mut status)",
    ".create(&mut status)"
)

with open(rust_file, "w") as f:
    f.write(content)
print("✅ Patched src/lib.rs (Bundle and Session Builder API updated)")

# 2. Inject tectonic_bundles dependency
with open(cargo_toml, "r") as f:
    cargo = f.read()

if "tectonic_bundles" not in cargo:
    # Safely inject directly under the dependencies header
    cargo = cargo.replace("[dependencies]", "[dependencies]\ntectonic_bundles = \"0.3\"", 1)
    with open(cargo_toml, "w") as f:
        f.write(cargo)
    print("✅ Injected 'tectonic_bundles' into Cargo.toml")
else:
    print("⚡ 'tectonic_bundles' is already present.")

print("🎉 Surgical repair complete!")
