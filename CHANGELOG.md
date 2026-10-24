# Changelog

## [1.1.0-2026.10.24] - 2026-10-24

### Scooter Cluster
- Place names from OpenStreetMap (denser than GeoNames; fixes one name over long distances)
- Place-name lookups cached within 200 m; failing Android Geocoder skipped for 10 min (was ~700 lookups/min while riding)
- Turn-arrow learning from Google Maps icons via files/icons/labels.txt (experimental)
- iQube weather hook removed (cluster never displayed it); NTorq XP keeps Open-Meteo

### Vehicle Card
- Driving mode shows Eco / Power; mode 0 hidden

### Vehicle Stats & Rides
- Charging history and SOC Analysis cards shown
- Charging record detail opens even though the charging-points API 404s in Nepal
- New ride starts after 5 min parked instead of 15 (untested)

### App-wide
- App-rating sheet and nudge counter disabled

### Commits
- 85de675 - Place-name cache, OSM places, stats cards, rating off, 5-min pause, arrow learning

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
