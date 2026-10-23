# TVS Connect (Nepal) — community fixes

Unofficial patches for the **TVS Connect Nepal** Android app (`com.tvsm.connect.nepal` 4.5.0, versionCode 42) used with the TVS iQube (and partly NTorq XP).
The patched app installs **side by side** with the Play Store app as `com.tvsm.connect.nepdb` ("TVS Connect - Debug"), so the original stays untouched.

This repo contains **no TVS code**. It holds our patches, our own helper code and build scripts. You supply your own copy of the app.

## What it fixes / adds

| Area | Change | Status |
|---|---|---|
| Home screen stale ("synced yesterday", stuck "On Ride") | process MQTT packets when not on Home tab; resubscribe on resume / pull-to-refresh; wider REST window | verified |
| Place search | removed India-only country filter; share `lat, lng` text (e.g. from Google Maps) to the app → plotted and navigable | verified |
| Ride detail map | live, zoomable HERE map with route instead of static image | verified |
| Ride times 15 min off | server sends IST; converted to phone time zone (list + detail) | verified |
| Location card | "Location as of h:mm" (TCU repeats last fix until GPS locks), "On Ride · 25 km/h · Eco", marker pinned to exact position | verified |
| Cluster location name offline | nearest GeoNames place when Android Geocoder has no internet | verified (offline ride pending) |
| Update popup | disabled in the clone (it can't be updated from Play anyway) | verified |
| Nav reroute after missed turn | engine route kept in sync after `returnToRoute` | **untested** |
| Google Maps navigation → scooter cluster | turn arrows/distance from Google Maps' notification sent over BLE | **experimental** |
| Cluster weather | Open-Meteo instead of TVS weather API (returns nothing for Nepal) | **experimental** |
| NTorq XP ride recording | GPS updates actually start; paused rides not deleted / merged | **untested** |
| Diagnostics | app log (JWT/email redacted) to `files/navlog.txt`, FCM/geofence logging | verified |

Not app-fixable (see `docs/findings.md`): geofence alerts (server never generates them), anti-theft (TCU/server), HERE POI and one-way data in Nepal.

Full per-file list: `docs/patches.md`.

## Build

Requirements: Linux/macOS, Java 21 (`javac` too), Python 3, `curl`, `zip`, `patch`, `adb`.

1. Get the three APKs of **exactly** version 4.5.0 from your own phone:
   ```sh
   adb shell pm path com.tvsm.connect.nepal          # lists base.apk + split_config.*.apk
   mkdir -p apk && adb pull <each path> apk/
   ```
   `base.apk` must match the hash in `scripts/apk.sha256` (other versions won't patch). Pull every `split_config.*.apk` your phone lists; they vary by screen density/ABI and are only renamed, not patched.
2. `scripts/fetch-tools.sh` — downloads apktool 2.11.1, uber-apk-signer 1.3.0, R8/D8 8.5.35 (hash-checked).
3. `scripts/setup.sh` — decodes and applies patches into `work/base`.
4. `scripts/build.sh` — builds signed APKs into `work/signed/`.
5. `scripts/install.sh` — installs next to the Play app and pushes `places.txt`.

Then log in with OTP in the clone and pair the scooter.

## Quick start with an AI coding agent

Prereqs: TVS Connect Nepal 4.5.0 installed from Play on your phone, USB debugging on, phone plugged in (`adb devices` lists it).
Clone this repo, open Claude Code / Codex / Cursor in it, and paste:

> Read AGENTS.md and README.md. Then build and install the patched TVS Connect clone on my connected phone: check prerequisites (java 21 + javac, python3, curl, zip, patch, adb) and tell me what's missing; pull the three 4.5.0 APKs from the phone into apk/ with adb; run scripts/fetch-tools.sh, scripts/setup.sh, scripts/build.sh, scripts/install.sh, stopping at the first error. Don't uninstall or modify the Play Store app. When done, tell me what to do on the phone.

To change something afterwards:

> Read AGENTS.md and docs/patches.md. I want <describe the problem/feature>. Find the relevant code (decompile apk/base.apk with jadx if needed), make the smallest patch in work/base (logic in src/java helpers), rebuild, install, and tell me how to test it. Then run scripts/mkpatch.sh and update docs/patches.md with status "untested".

## Caveats

- Signed with uber-apk-signer's debug key. Anyone with that key can sign an "update"; only install builds you made.
- FCM pushes (geofence/theft alerts) for the clone depend on the TVS server holding the clone's token; keep the Play app for alerts.
- The clone trusts user-installed CAs (so you can inspect your own traffic with mitmproxy).
- Built and tested on one Samsung S20 FE (Android 13) + iQube, and one Xiaomi (Android 13) + NTorq XP.

## Contributing

Human or AI agent: read `AGENTS.md` first.

License: MIT for our code; see `NOTICE.md`.
