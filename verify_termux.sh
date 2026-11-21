#!/bin/bash

echo "🔍 Scanning Termux Environment..."
echo "-----------------------------------"

# 1. Verify XeLaTeX Binary Path
TEX_PATH="/data/data/com.termux/files/usr/bin/xelatex"
if [ -x "$TEX_PATH" ]; then
    echo "✅ XeLaTeX binary found and executable at: $TEX_PATH"
else
    echo "❌ XeLaTeX NOT found or not executable!"
    echo "   -> Run this to fix: pkg install texlive-xetex"
fi

# 2. Verify External App Execution Policy
PROP_FILE="$HOME/.termux/termux.properties"
mkdir -p ~/.termux

if grep -q "^allow-external-apps *= *true" "$PROP_FILE" 2>/dev/null; then
    echo "✅ allow-external-apps is already set to true."
else
    echo "⚠️ allow-external-apps is missing or false. Repairing..."
    # Remove any existing false entries
    sed -i '/^allow-external-apps/d' "$PROP_FILE" 2>/dev/null
    # Append the correct rule
    echo "allow-external-apps = true" >> "$PROP_FILE"
    termux-reload-settings
    echo "✅ Rule injected and Termux settings reloaded!"
fi
echo "-----------------------------------"
echo "🚀 Termux scan complete."
