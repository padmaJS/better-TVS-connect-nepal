#!/usr/bin/env python3
"""Assemble side-by-side clone: original APKs + patched dex + same-length byte-patched package name.
Usage: build_clone.py  (run from repo root via scripts/build.sh; needs apk/*.apk, work/apks/dex-only.apk)"""
import zipfile, os
OLD, NEW = b"com.tvsm.connect.nepal", b"com.tvsm.connect.nepdb"
PERM_OLD, PERM_NEW = b"com.tvsm.EXPORTED_PERMISSION", b"com.tvsm.EXPORTED_PERMISSIOM"
LBL_OLD, LBL_NEW = b"TVS Connect - Nepal", b"TVS Connect - Debug"
assert len(OLD)==len(NEW) and len(PERM_OLD)==len(PERM_NEW) and len(LBL_OLD)==len(LBL_NEW)

def sub(data, pairs):
    for o, n in pairs:
        for enc in ("utf-8", "utf-16-le"):
            oo = o.decode().encode(enc); nn = n.decode().encode(enc)
            data = data.replace(oo, nn)
    return data

def build(src, dst, patch):
    zi = zipfile.ZipFile(src)
    zo = zipfile.ZipFile(dst, "w")
    for i in zi.infolist():
        if i.filename.startswith("META-INF/") and i.filename.upper().endswith((".SF",".RSA",".DSA",".EC",".MF")):
            continue
        data = patch(i.filename, zi.read(i.filename))
        ni = zipfile.ZipInfo(i.filename, i.date_time)
        ni.compress_type = i.compress_type
        ni.external_attr = i.external_attr
        zo.writestr(ni, data)
    zo.close()

dex = zipfile.ZipFile("work/apks/dex-only.apk")
nsc = open("assets/nsc.bin", "rb").read()
def base_patch(name, data):
    if name.startswith("classes") and name.endswith(".dex"):
        return dex.read(name)
    if name == "AndroidManifest.xml":
        return sub(data, [(OLD, NEW), (PERM_OLD, PERM_NEW)])
    if name == "resources.arsc":
        return sub(data, [(OLD, NEW), (LBL_OLD, LBL_NEW)])
    if name == "res/xml/network_security_config.xml":
        return nsc
    return data
def split_patch(name, data):
    if name in ("AndroidManifest.xml", "resources.arsc"):
        return sub(data, [(OLD, NEW)])
    return data

os.makedirs("work/apks2", exist_ok=True)
build("apk/base.apk", "work/apks2/base.apk", base_patch)
build("apk/split_config.arm64_v8a.apk", "work/apks2/split_arm64_v8a.apk", split_patch)
build("apk/split_config.xxhdpi.apk", "work/apks2/split_xxhdpi.apk", split_patch)
print("ok")
