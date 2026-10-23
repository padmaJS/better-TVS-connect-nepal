#!/bin/sh
# After editing work/base: regenerate patches/smali.diff and copy hand-written com/dbg smali back to src/smali.
# G/N smali are generated from src/java (edit the .java, run scripts/helpers.sh) and are not copied back.
set -e
cd "$(dirname "$0")/.."
(cd src/smali && find . -name '*.smali') | while read f; do cp "work/base/$f" "src/smali/$f"; done
cd work && rm -f a b && ln -s orig a && ln -s base b
diff -ruN -x dbg -x build -x apktool.yml -x original -x AndroidManifest.xml -x res -x unknown -x assets -x kotlin -x META-INF a/ b/ \
  | sed -E 's/^(\+\+\+|---) ([^\t]+)\t.*/\1 \2/' > ../patches/smali.diff || true
rm a b
grep -c '^+++' ../patches/smali.diff | xargs echo "files patched:"
