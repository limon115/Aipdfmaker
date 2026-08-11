#!/bin/bash
set -e

echo "Building tectonic-jni for Android architectures..."

# Navigate to the rust crate directory
cd app/src/main/rust/tectonic-jni

# Verify cargo ndk is installed
if ! command -v cargo-ndk &> /dev/null; then
    echo "cargo-ndk could not be found. Installing..."
    cargo install cargo-ndk
fi

# Build for all targets, outputting to the Android jniLibs folder
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../../../jniLibs build --release

echo "Done! The .so files have been placed in app/src/main/jniLibs/"
