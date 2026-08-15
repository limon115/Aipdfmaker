import os

cargo_toml = "app/src/main/rust/tectonic-jni/Cargo.toml"

if os.path.exists(cargo_toml):
    with open(cargo_toml, "r") as f:
        content = f.read()

    # Unlock the bundles crate to perfectly match the rest of the ecosystem
    content = content.replace('tectonic_bundles = "0.3"', 'tectonic_bundles = "*"')

    with open(cargo_toml, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Unlocked tectonic_bundles to '*'")
else:
    print("❌ Cargo.toml not found!")
