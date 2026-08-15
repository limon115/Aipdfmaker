import os, glob

workflows = glob.glob(".github/workflows/*.yml")
if not workflows:
    print("❌ No YAML files found in .github/workflows/")
else:
    for w in workflows:
        with open(w, "r") as f:
            content = f.read()

        start_str = 'echo "Extracting vendored C++ tarballs..."'
        end_str = 'export NDK='
        
        start_idx = content.find(start_str)
        end_idx = content.find(end_str)

        if start_idx != -1 and end_idx != -1:
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
          extract_or_download "icu4c-73_2-src.tgz" "https://github.com/unicode-org/icu/releases/download/release-73-2/icu4c-73_2-src.tgz"
          
          """
            
            # Turn on Git LFS just in case they are LFS pointers
            if "lfs: true" not in content:
                content = content.replace("uses: actions/checkout@v4", "uses: actions/checkout@v4\n        with:\n          lfs: true")
                
            new_content = content[:start_idx] + new_block + content[end_idx:]
            with open(w, "w") as f:
                f.write(new_content)
            print(f"✅ SUCCESS: Patched {w} using smart string targeting!")
        else:
            print(f"⚠️ {w} found, but could not locate the exact boundaries to replace.")
