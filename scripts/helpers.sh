#!/bin/sh
# Compile src/java/com/dbg/{G,N}.java (against stubs) -> dex -> smali into work/base/smali_classes16/com/dbg.
# Stubs only satisfy javac; real classes come from the app at runtime. com/dbg/F is hand-written smali (stubbed here).
set -e
cd "$(dirname "$0")/.."
T=work/helpers; rm -rf $T; mkdir -p $T/cls $T/dex
javac -nowarn --release 8 -d $T/cls -sourcepath src/java/stub src/java/com/dbg/G.java src/java/com/dbg/N.java
java -cp tools/r8.jar com.android.tools.r8.D8 --min-api 28 --output $T/dex $(ls $T/cls/com/dbg/*.class | grep -E '/(G|N)(\$[0-9]+)?\.class$')
(cd $T/dex && zip -q ../h.apk classes.dex)
java -jar tools/apktool.jar d -r -f -o $T/dec $T/h.apk
cp $T/dec/smali/com/dbg/*.smali work/base/smali_classes16/com/dbg/
