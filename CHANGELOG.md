# Changelog

## [1.0.0-2026.10.23] - 2026-10-23

First public release. Patches TVS Connect Nepal 4.5.0 (vc 42) into a side-by-side clone (com.tvsm.connect.nepdb); bring your own APK.

### Home Screen Sync
- Live MQTT packets processed even when Home tab not visible (no more stuck "On Ride" / "synced yesterday")
- MQTT resubscribe on resume and pull-to-refresh; wider REST fallback window

### Vehicle Card
- "Location as of h:mm" instead of hard-coded "Live Location"
- "On Ride · speed · mode" from live GPS speed
- Map marker pinned to exact scooter position

### Search & Navigation
- Removed India-only search filter
- Share "lat, lng" text (e.g. from Google Maps) to plot and navigate
- Reroute keeps engine route in sync (untested)

### Ride Stats
- Live zoomable ride map with route
- Ride times converted from IST to phone time zone

### Scooter Cluster
- Offline place name from GeoNames when no internet
- Google Maps turn-by-turn to cluster (experimental)
- Weather from Open-Meteo (experimental)

### NTorq XP
- GPS ride tracking starts reliably; paused rides no longer deleted or merged (untested)

### App
- Update popup disabled in clone
- Redacted diagnostic log (navlog.txt) and FCM logging

### Tooling
- Scripts: fetch-tools, setup, build, install, mkpatch
- Works with any density/ABI split APKs; only base.apk hash-checked
- AGENTS.md guide for AI coding agents; docs/patches.md and docs/findings.md

### Commits
- 5f695a1 - Initial release: TVS Connect Nepal 4.5.0 patches
- 79cab43 - init commit
