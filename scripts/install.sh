#!/bin/sh
# Install the clone next to the Play app. Set ANDROID_SERIAL if several devices. --user 0 skips work/secure profiles.
set -e
cd "$(dirname "$0")/.."
adb install-multiple --user 0 -r --no-incremental work/signed/*-aligned-debugSigned.apk
adb push assets/places.txt /sdcard/Android/data/com.tvsm.connect.nepdb/files/places.txt
