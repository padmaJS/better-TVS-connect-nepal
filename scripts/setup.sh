#!/bin/sh
# Decode your own APKs (apk/*.apk, TVS Connect Nepal 4.5.0) and apply patches -> work/base.
# work/orig = pristine decode (used by mkpatch.sh), work/base = patched tree you edit.
set -e
cd "$(dirname "$0")/.."
(cd apk && grep " base.apk" ../scripts/apk.sha256 | sha256sum -c -) || { echo "apk/ must hold base.apk of exactly 4.5.0 (vc 42), see README"; exit 1; }
rm -rf work/orig work/base
java -jar tools/apktool.jar d -r -f -o work/orig apk/base.apk
cp -r work/orig work/base
patch -p1 -s -d work/base < patches/smali.diff
cp -r src/smali/. work/base/
scripts/helpers.sh
echo "setup ok: edit work/base, then scripts/build.sh"
