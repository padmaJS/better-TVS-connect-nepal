# Agent guide

Read this, then `docs/patches.md` (what is patched where) and `docs/findings.md` (how the app works).

## Layout

| Path | What | Edit? |
|---|---|---|
| `patches/smali.diff` | all changes to TVS smali (unified diff vs. pristine apktool decode) | regenerate via `scripts/mkpatch.sh`, don't hand-edit |
| `src/smali/` | hand-written helper classes `com/dbg/{Coords,F,L,NoIntercept,RetryDraw}` | yes (or edit in `work/base` + mkpatch) |
| `src/java/com/dbg/{G,N}.java` | helper logic in Java, compiled to smali by `scripts/helpers.sh` | yes — edit Java, not the generated smali |
| `src/java/stub/` | compile-only stubs of app/Android classes G/N call | add a stub when you call a new app method |
| `assets/` | `places.txt` (GeoNames), `nsc.bin` (network security config) | rarely |
| `scripts/` | fetch-tools, setup, helpers, build, install, mkpatch, `build_clone.py` | |
| `apk/`, `work/`, `tools/` | user's APKs, decoded trees, jars — **gitignored** | never commit |

## Loop

```sh
scripts/fetch-tools.sh && scripts/setup.sh    # once (needs apk/*.apk, see README)
# edit work/base/smali_classesN/... and/or src/java/com/dbg/*.java
scripts/helpers.sh                             # only if Java changed
scripts/build.sh && scripts/install.sh         # needs a device (ANDROID_SERIAL)
scripts/mkpatch.sh                             # before committing: refresh patches/smali.diff + src/smali
```

Find code: decompile `apk/base.apk` with jadx to read Java, then patch the matching smali in `work/base`. Class/method names below are as they appear in smali (obfuscated short names like `MainActivity.L3`, `NavigationFragment.b4`).

## Verify

- Helpers have self-checks (no framework):
  `java -Dpos.path=/tmp/pos -cp work/helpers/cls com.dbg.G` · `java -Dgeo.path=assets/places.txt -cp work/helpers/cls com.dbg.G 27.6721 85.4281` (→ Bhaktapur) · `java -cp work/helpers/cls com.dbg.N` (→ N OK).
- Build must succeed; on device: app launches, `adb logcat -b crash` empty, `adb shell cat /sdcard/Android/data/com.tvsm.connect.nepdb/files/navlog.txt | grep <your tag>`.
- Mark anything not tried on a real scooter as **untested** in `docs/patches.md` and README.

## Rules

- **Never commit**: APKs, dex, decompiled Java/smali beyond the diff, resources/icons from the app, `navlog.txt`/logcat (contain GPS tracks, JWT, email, phone numbers), device serials, real coordinates of anyone's home. Use generic coordinates (e.g. 27.7000, 85.3000) in tests.
- Patches stay **surgical**: smallest smali change; put real logic in `com/dbg` helpers called with one `invoke-static`.
- New helper classes go in `smali_classes16` (`smali_classes3` is at the 64K method-ref limit).
- Log via `com/dbg/F.a(String)` → navlog.txt with a greppable TAG prefix. Never log tokens.
- Don't touch the Play app's package or data. Clone package is `com.tvsm.connect.nepdb` (same length as `nepal`; `build_clone.py` byte-patches manifest/arsc, so names must keep length).

## Gotchas (learned the hard way)

- **Don't rebuild resources with apktool.** It drops the HERE SDK's non-0x7f resource package → map/ride crashes. We build dex only (`apktool b --no-res`) and `build_clone.py` splices it into the original APKs.
- Keep `META-INF/services/*` (kotlinx coroutines Main dispatcher); strip only signature files.
- Provider authorities in smali are renamed `nepal.provider` → `nepdb.provider` (part of the diff); new code referencing providers must use the `nepdb` name.
- `MainActivity.isNavigationLocked()` is true until vehicle details load; patching the getter globally crashed `onCreate` — patch the call site.
- HERE: draw only after the map scene is loaded (`RetryDraw` queues the call). `visualNavigator` must be driven from the main thread.
- Server timestamps (`/api/ride`) are IST, MQTT `datetime_pkt` is UTC, `can_modified` is local; Nepal is UTC+5:45.
- `speed_can`/`odometer_can` refresh only every ~2 min; use GPS `speed` for live speed.
- Secure Folder / work profiles: always `--user 0` with `pm`/`adb install`.
- MIUI: needs "Install via USB" in developer options; it also blocks `adb input`.
