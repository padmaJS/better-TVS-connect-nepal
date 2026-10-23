#!/bin/sh
# work/base -> signed side-by-side clone APKs in work/signed (package com.tvsm.connect.nepdb).
set -e
cd "$(dirname "$0")/.."
java -jar tools/apktool.jar b --no-res -o work/apks/dex-only.apk work/base
rm -rf work/apks2
python3 scripts/build_clone.py
rm -rf work/signed
java -jar tools/uber-apk-signer.jar -a work/apks2 -o work/signed --allowResign
ls work/signed/*-aligned-debugSigned.apk
