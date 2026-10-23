#!/bin/sh
# Download pinned build tools into tools/ (gitignored) and verify hashes.
set -e
cd "$(dirname "$0")/.."
mkdir -p tools
get() { [ -f "tools/$1" ] || curl -fsSL -o "tools/$1" "$2"; }
get apktool.jar https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar
get uber-apk-signer.jar https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar
get r8.jar https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.5.35/r8-8.5.35.jar
cd tools && sha256sum -c ../scripts/tools.sha256
