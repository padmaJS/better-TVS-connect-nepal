# Patches

Paths are relative to `work/base/`. "smali" = edited TVS class (in `patches/smali.diff`); "helper" = our code in `src/`.
Status: **verified** = tried on device with a scooter; **untested** = builds and launches only.

## Build-level (not in smali)

| Change | Where | Status |
|---|---|---|
| Package `com.tvsm.connect.nepal` → `com.tvsm.connect.nepdb`, permission `…EXPORTED_PERMISSION` → `…PERMISSIOM`, label "TVS Connect - Debug" | `scripts/build_clone.py` (same-length byte patch of binary manifest/arsc) | verified |
| Trust user CAs | `assets/nsc.bin` replaces `res/xml/network_security_config.xml` | verified |
| Content provider authority rename | 18 smali files, only `nepal.provider` → `nepdb.provider` strings (share dialogs, `TVSMProvider`, `ReportsColumns`, `IntroVideo`, …) | verified |

## Home screen sync (#2/#5)

| Change | Where | Status |
|---|---|---|
| `onMQTTDataReceived` no longer drops packets unless fragment RESUMED + Home tab visible | smali_classes4 `ev/ui/dashboard/DashboardFragment` | verified |
| Pull-to-refresh (`r1`) also calls `MainActivity.callCommonDeviceDetailsApi()` | same | verified |
| REST `getlatestdevicedata` window starts today−7d (was today 00:00) | smali_classes4 `ev/ui/Common.getFormattedDateStrings` | verified (server returns RecordNotFound anyway) |
| `startP360SubscriptionFlow` + `onResume` always refresh; `callCommonDeviceDetailsApi` re-runs MQTT connect/subscribe | smali_classes4 `ev/MainActivity` | verified |
| MQTT: resubscribe when already connected (broker resends retained = latest packet); on `connect` failure while connected, subscribe anyway. Logs `MQTT_RESUB` | smali_classes3 `onboarding/datalayer/MQTTDataSource`, `MQTTDataSource$reconnectAndSubscribe$1` | verified |

## Vehicle card

| Change | Where | Status |
|---|---|---|
| Track time lat/lng last changed (`G.lp`, state in `files/lastpos.txt`, logs `LOC_ASOF`) | smali_classes4 `ev/MainActivity$k` → helper `G.lp` | verified |
| "Live Location" → "Location as of h:mm a" (`G.ll`); title "NAME · as of X" (`G.lt`) | smali_classes14 `core/maps/presentation/ev/compose/VehicleCardKt` | verified |
| "On Ride · 25 km/h · Eco" from GPS `speed` + `driving_mode` (`G.ls`; mode "1" = Eco, others shown as "Mode n") | same | verified |
| Card marker at exact position, bottom-centre anchor (was ~63 m north offset) | smali_classes16 `feature/heremaps/presentation/fragments/HereMapsFragment.f0` | verified |

## Search / navigation (#3/#4)

| Change | Where | Status |
|---|---|---|
| Removed `CountryCode.IND` filter in keyboard search | smali_classes14 `mapnavigation/framework/ImpNavigationDataSource.getMultiLocationResults` | verified |
| `isLocationOutSideIndia` returns false (old code hung when reverse-geocode list null) | same; smali_classes4 `NavigationFragment.x2` | verified |
| Shared text `lat, lng` (≥3 decimals) → plotted destination | helper `Coords` hooked in smali_classes3 `OnBoardingActivityNew.t`; `MainActivity.L3` no longer needs a short URL; `NavigationFragment` registers observers at cold start (onViewCreated check of `isNavigationLocked`) | verified |
| Reroute success also updates `engine.route` so resume doesn't restore the old route | smali_classes14 `HereNavigationEngine$handleRouteDeviationListener$1$4$1$1` | **untested** |
| Log `returnToRoute` result `REROUTE_RESULT` | smali_classes14 `navigation_sources/sdk/q` | verified (log only) |
| Debug markers `DBGSHARE` (`com/dbg/L`) in share path | `SharedViewModel`, `NavigationFragment`, `NavigationFragment$l`, `MainActivity` | leftover debug; safe to remove |

## Ride stats (#6, time zone)

| Change | Where | Status |
|---|---|---|
| Live zoomable HERE map + route (3 `GOOGLE_MAPS` checks + inverted `HERE_MAPS` check neutralised) | smali_classes14 `vehiclestats/ui/fragment/RideDetailFragment` | verified |
| `drawRoute` resolves colour via ContextCompat; waits for scene load (`RetryDraw`); `NoIntercept` stops ScrollView stealing map gestures | smali_classes16 `HereMapsFragment`; helpers `RetryDraw`, `NoIntercept` | verified |
| Ride times IST → phone time zone (`G.a1/a2` wrap `DateTimeUtils.changeDateTimeFormatInIST*`) | `RideDetailFragment`, `RideTripListFragment`, `vehiclestats/ui/adapter/RideTripListAdapter` | verified (SoC chart labels not checked) |

## Cluster (BLE)

| Change | Where | Status |
|---|---|---|
| Offline place name: on Geocoder IOException/empty → `G.n` nearest GeoNames place ≤15 km. Logs `OFFLINE_GEO` | smali_classes13 `tvs/bluetooth/core/utils/BluetoothUtil$getAddressFromLatLng$2` | verified online; offline ride pending |
| Google Maps nav notification → cluster arrows/distance/road name (`N.p`/`N.r`, U546 icon ids, resend 1 s). Logs `GMAPS_NAV` | smali_classes13 `tvs/ble/feature/services/NotificationListenerService` (iQube), `tvsm/connect/utils/NotificationReceiverService` (XP) | **experimental**: frames sent, arrow mapping being verified |
| Icon/text survey: `touch files/picto_test`, start a Maps nav → cluster cycles icons 0–63 then text frames | helper `N` | survey tool |
| Weather from Open-Meteo (`N.w` iQube, `N.w2` XP; position rounded to 2 dp, cached 15 min / 3 h) | smali_classes13 `tvs/bluetooth/core/utils/BluetoothUtil.createClimate`; smali_classes15 `tvsm/connect/bluetooth/BluetoothUtil.createClimate` | **experimental**: frames reach cluster, nothing displayed yet |

## NTorq XP (vehicleTypeId 9, old N251 stack)

| Change | Where | Status |
|---|---|---|
| Location updates start without waiting for `GoogleApiClient.isConnected` (was async → never started; whole ride one point) | smali_classes13 `tvsm/connect/utils/LocationTracker` | **untested** |
| Paused ride no longer deleted on API error | smali_classes15 `tvsm/connect/DBHelper.updateN251BleRideStatusForpause` | **untested** |
| Paused ride >1 h old → closed + synced instead of merged into next ride (`N.pz`) | smali_classes15 `N251ClusterDataReceiver.G0`; smali_classes5 `unified/ui/activities/UniDashboardActivity.onResume` | **untested** |
| Ride detail: if server omits `travelFileName`, derive it from the download link so the route CSV loads (`N.rd`, logs `RIDE detail`) | smali_classes15 `bikedashboard/n251/U458RideDetailFragment` | **untested** |

## App-wide

| Change | Where | Status |
|---|---|---|
| No update / force-update dialog (`checkAppVersion` returns immediately) | smali_classes15 `tvsm/connect/TVSApplication` | verified |
| All Timber lines (+ redaction of JWT/bearer/email) appended to `files/navlog.txt` (helper `F`, rotates at 50 MB) | smali_classes12 `tvs/logs/ReleaseLogTree` | verified |
| FCM logging: `FCM_MSG`, `FCM_NEW_TOKEN`, `FCM_TOKEN ok/len` (never the token) | smali_classes3 `fcm/MyFirebaseMessagingService`; smali_classes15 `SplashActivity$a`; `MainActivity.onResume` | verified |
