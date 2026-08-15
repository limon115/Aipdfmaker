import os, re

rust_file = "app/src/main/rust/tectonic-jni/src/lib.rs"

if os.path.exists(rust_file):
    with open(rust_file, "r") as f:
        content = f.read()

    # Step 1: Initialize the builder as its own variable
    content = re.sub(
        r"let\s+mut\s+session\s*=\s*ProcessingSessionBuilder::default\(\)",
        "let mut builder = ProcessingSessionBuilder::default();\n    builder",
        content
    )

    # Step 2: Terminate the configuration chain and hand ownership to create()
    content = re.sub(
        r"\.create\(\s*&mut\s+status\s*\)",
        ";\n    let mut session = builder.create(&mut status)",
        content
    )

    with open(rust_file, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Appeased the Rust Borrow Checker by splitting builder ownership.")
else:
    print("❌ lib.rs not found!")
