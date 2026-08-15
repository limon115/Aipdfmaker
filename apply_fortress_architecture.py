import glob

# ==========================================
# 1. REWRITE THE GITHUB ACTIONS WORKFLOW
# ==========================================
workflow_yaml = r"""name: DocMorph Cloud Compilation

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    name: Cloud Build APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Install Rust & cargo-ndk
        run: |
          rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
          cargo install cargo-ndk

      - name: Download Tectonic TeX Bundle
        run: |
          mkdir -p app/src/main/assets/tectonic-bundle
          echo "Downloading Tectonic TeX Live bundle..."
          curl -L -o app/src/main/assets/tectonic-bundle/default.ttb "https://github.com/tectonic-typesetting/tectonic-texlive-bundles/releases/download/tlextras-2021.3r1/tlextras-2021.3r1.tar"

      - name: Build Static zlib and libpng for Android NDK
        run: |
          wget -q https://zlib.net/fossils/zlib-1.2.13.tar.gz
          tar xf zlib-1.2.13.tar.gz
          wget -q https://download.sourceforge.net/project/libpng/libpng16/1.6.40/libpng-1.6.40.tar.gz
          tar xf libpng-1.6.40.tar.gz
          
          export NDK=$ANDROID_NDK_HOME
          export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
          export API=24
          
          for arch in aarch64-linux-android armv7a-linux-androideabi x86_64-linux-android i686-linux-android; do
            mkdir -p zlib-build-$arch && cd zlib-build-$arch
            export CC=$TOOLCHAIN/bin/${arch}${API}-clang
            export AR=$TOOLCHAIN/bin/llvm-ar
            export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
            ../zlib-1.2.13/configure --prefix=$GITHUB_WORKSPACE/deps/$arch --static
            make -j4 install
            cd ..
            
            mkdir -p png-build-$arch && cd png-build-$arch
            export CPPFLAGS="-I$GITHUB_WORKSPACE/deps/$arch/include"
            export LDFLAGS="-L$GITHUB_WORKSPACE/deps/$arch/lib"
            ../libpng-1.6.40/configure --host=$arch --prefix=$GITHUB_WORKSPACE/deps/$arch CC=$CC AR=$AR RANLIB=$RANLIB --disable-shared --enable-static
            make -j4 install
            cd ..
          done

      - name: Build Tectonic JNI (Rust)
        env:
          CARGO_BUILD_JOBS: "2"
          PKG_CONFIG_ALLOW_CROSS: "1"
          PKG_CONFIG_PATH_aarch64_linux_android: "${{ github.workspace }}/deps/aarch64-linux-android/lib/pkgconfig"
          PKG_CONFIG_PATH_armv7_linux_androideabi: "${{ github.workspace }}/deps/armv7a-linux-androideabi/lib/pkgconfig"
          PKG_CONFIG_PATH_x86_64_linux_android: "${{ github.workspace }}/deps/x86_64-linux-android/lib/pkgconfig"
          PKG_CONFIG_PATH_i686_linux_android: "${{ github.workspace }}/deps/i686-linux-android/lib/pkgconfig"
        run: |
          cd app/src/main/rust/tectonic-jni
          cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../../../libs build --release

      - name: Generate Gradle Wrapper (Cloud Execution)
        run: gradle wrapper

      - name: Generate Debug Keystore Dynamically
        run: |
          keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "C=US, O=Android, CN=Android Debug"
      - name: Grant Execute Permissions
        run: chmod +x gradlew || true

      - name: Assemble Debug APK with Gradle
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload DocMorph APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: DocMorph-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
"""
workflow_files = glob.glob(".github/workflows/*.yml")
if workflow_files:
    with open(workflow_files[0], 'w') as f:
        f.write(workflow_yaml)
    print("✅ Workflow patched: Native zlib + libpng pipeline & OOM prevention injected.")

# ==========================================
# 2. REWRITE TECTONIC BRIDGE (MEMORY SAFE)
# ==========================================
kotlin_code = r"""package com.example.domain.services.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TectonicBridge {
    init {
        try {
            System.loadLibrary("tectonic_jni")
        } catch (e: UnsatisfiedLinkError) {
            com.example.utils.AppLogger.e("TectonicBridge", "Could not load tectonic_jni library. Have you compiled the Rust crate and placed .so in jniLibs?", e)
        }
    }
    external fun compileToPdf(texSource: String, bundlePath: String, outputDir: String): String

    suspend fun compileLatex(context: Context, tex: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val bundlePath = ensureBundleExtracted(context)
            val outDir = context.cacheDir.absolutePath
            val resultPath = compileToPdf(tex, bundlePath, outDir)
            if (resultPath == "Error") {
                throw Exception("Tectonic compilation failed")
            }
            File(resultPath)
        }
    }

    private fun ensureBundleExtracted(context: Context): String {
        val bundleDir = File(context.filesDir, "tectonic-bundle")
        if (!bundleDir.exists()) {
            bundleDir.mkdirs()
            try {
                val assets = context.assets.list("tectonic-bundle") ?: emptyArray()
                for (asset in assets) {
                    context.assets.open("tectonic-bundle/$asset").use { input ->
                        File(bundleDir, asset).outputStream().use { output ->
                            // Explicit 16KB chunked buffering prevents OOM crashes
                            // when unpacking massive assets on low-RAM devices
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                com.example.utils.AppLogger.e("TectonicBridge", "Error extracting tectonic bundle", e)
            }
        }
        return bundleDir.absolutePath
    }
}
"""
with open("app/src/main/java/com/example/domain/services/pdf/TectonicBridge.kt", 'w') as f:
    f.write(kotlin_code)
print("✅ TectonicBridge patched: Chunked byte-buffer stream injected.")
