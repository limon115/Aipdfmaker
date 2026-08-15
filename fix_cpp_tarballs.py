import os, re

workflow = ".github/workflows/android_build.yml"
if os.path.exists(workflow):
    with open(workflow, "r") as f:
        content = f.read()
    
    # Using re.DOTALL to capture the multi-line block perfectly
    pattern = re.compile(r'echo "Extracting vendored C\+\+ tarballs\.\.\.".*?cp vendor_tars/icu4c-73_2-src\.tgz \. && tar xf icu4c-73_2-src\.tgz', re.DOTALL)
    
    new_block = """echo "Extracting C++ tarballs (with dynamic network fallback for corrupted files)..."
          
          extract_or_download() {
              local file=$1
              local url=$2
              cp "vendor_tars/$file" . 2>/dev/null || true
              if ! tar xf "$file" 2>/dev/null; then
                  echo "⚠️ $file is corrupted (HTML/LFS pointer)! Downloading real binary..."
                  curl -fL --retry 3 -o "$file" "$url"
                  tar xf "$file"
              else
                  echo "✅ Successfully extracted $file"
              fi
          }

          extract_or_download "zlib-1.2.13.tar.gz" "https://github.com/madler/zlib/releases/download/v1.2.13/zlib-1.2.13.tar.gz"
          extract_or_download "libpng-1.6.40.tar.gz" "https://downloads.sourceforge.net/project/libpng/libpng16/1.6.40/libpng-1.6.40.tar.gz"
          extract_or_download "graphite2-1.3.14.tgz" "https://github.com/silnrsi/graphite/releases/download/1.3.14/graphite2-1.3.14.tgz"
          extract_or_download "freetype-2.13.2.tar.gz" "https://download.savannah.gnu.org/releases/freetype/freetype-2.13.2.tar.gz"
          extract_or_download "harfbuzz-8.3.0.tar.xz" "https://github.com/harfbuzz/harfbuzz/releases/download/8.3.0/harfbuzz-8.3.0.tar.xz"
          extract_or_download "expat-2.5.0.tar.gz" "https://github.com/libexpat/libexpat/releases/download/R_2_5_0/expat-2.5.0.tar.gz"
          extract_or_download "fontconfig-2.14.2.tar.gz" "https://www.freedesktop.org/software/fontconfig/release/fontconfig-2.14.2.tar.gz"
          extract_or_download "icu4c-73_2-src.tgz" "https://github.com/unicode-org/icu/releases/download/release-73-2/icu4c-73_2-src.tgz" """

    if pattern.search(content):
        content = pattern.sub(new_block, content)
        
        # Turn on Git LFS in the checkout step just in case they are LFS pointers
        if "lfs: true" not in content:
            content = content.replace("uses: actions/checkout@v4", "uses: actions/checkout@v4\n        with:\n          lfs: true")

        with open(workflow, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Multi-line regex matched! Injected dynamic C++ network fallback logic into android_build.yml.")
    else:
        print("⚠️ Could not find the exact extraction block in android_build.yml. It may have already been patched.")
else:
    print("❌ Could not find android_build.yml")
