# Findings

Target: `com.tvsm.connect.nepal` 4.5.0 (vc 42), internal build `TVSConnect-8.8.0_nepalProdRelease`, minSdk 28 / target 35. Native Java/Kotlin, 17 dex; bundles HERE SDK, Mappls, Cerence voice, Realm, SQLCipher.
Class names: jadx/smali names from that build (obfuscated members change between versions).

## Architecture

- **Home screen data (iQube/EV)**: MQTT over TLS to Trak N Tell (`ssl://psexts.trakntell.com:8883`, Paho, topic `<telOrgId>/<telVehicleId>`, retained message = latest packet) **or** P360 GraphQL (`wss://p360.tvsmotor.com`), plus one-shot REST `GET /api/home/getlatestdevicedata`. In our tests REST always returned `RecordNotFound`, so MQTT is the only real source.
- **BLE** phone ↔ scooter cluster only (nav arrows, notifications, location name, weather). Home screen doesn't use it.
- **Maps**: HERE for tiles, search, routing, turn-by-turn on iQube (`MainActivity` forces `MapProviderType.HERE_MAPS`); Mappls only on ICE screens.
- **Alerts** (geofence, theft, fall, crash): evaluated server-side, delivered only via FCM (`fcm/MyFirebaseMessagingService`; keys `stringified/alertType/notiType`). No on-device geofencing.
- REST base `tvsconnectapi.tvsmotor.com/api/…`; app trusts system CAs only and `allowBackup=false`.

## Bugs found (root causes)

### Home screen stale / stuck "On Ride"
1. `DashboardFragment.onMQTTDataReceived` processed a packet only if the fragment was RESUMED and the Home tab visible; otherwise discarded. Ignition-off packet arriving in background → stuck "On Ride" forever.
2. REST fallback window = today 00:00–23:59 (`Common.getFormattedDateStrings`); called once on dashboard setup, never on pull-to-refresh.
3. Long-lived process: MQTT stays "connected" but delivers nothing (cleanSession + autoReconnect, no resubscribe). New activity → `connect()` fails with 32100 "Client is connected" → `onFailure` posts the cached prefs packet (stale).
4. `new JSONObject(mqttData)` outside try; retry caps (P360 3, TNT 4) never reset; errors swallowed.

### Location card
- "Live Location" label hard-coded (`VehicleCardInfo.locationType` default `LIVE_LOCATION`); server `isLiveLocation/isNoGps/lastValidTimestamp` ignored; missing lat/lng → 0,0.
- TCU repeats the last-known lat/lng for 1–3.5+ min after ignition until GPS fix (`heading` 0.0 meanwhile). Packet has no GPS-valid field.
- `speed_can`/`odometer_can` refresh every ~2 min (`can_modified`); GPS `speed` (int km/h) updates every packet.
- Card marker placed ~63 m north of the scooter (fixed lat offset in `HereMapsFragment.f0`).

### Search / navigation
- `ImpNavigationDataSource.getMultiLocationResults` adds `CountryCode.IND` → keyboard search India-only. Autosuggest has no filter but HERE POI coverage in Nepal is thin (data, not app).
- `isLocationOutSideIndia` hangs forever when reverse-geocode result list is null.
- Launcher `OnBoardingActivityNew` accepts `geo:` VIEW and `text/*` SEND but only forwarded short URLs; observers in `NavigationFragment.b4` registered only if `!MainActivity.isNavigationLocked()`, which is true at cold start.
- Reroute: `RouteDeviationListener` (>30 m off route) → online-only `routingEngine.returnToRoute`. Success sets `visualNavigator` route (from `Dispatchers.IO`) but not `engine.route`; `newReroute` has no collector; on resume nav restarts with the original route. Instruction text only updates from `RouteProgressListener`, so off-route = frozen last maneuver. One-way violations come from HERE road data (routing uses ScooterOptions).

### Ride stats
- Ride map is a static HERE Map Image PNG unless provider == GOOGLE_MAPS (`RideDetailFragment`), so no zoom. `HereMapsFragment.drawRoute` passed a colour resource id to `Color.valueOf`.
- `/api/ride` start/end timestamps are IST (UTC+5:30), shown raw → 15 min behind Nepal time. `vehiclestats` `DateTimeUtils` hard-codes `Asia/Kolkata`; many `GMT+05:30` in `connect/utils/Utils`.

### Geofence / anti-theft (server-side, not fixable in app)
- Endpoints: `/api/geofence/{addgeofence,updategeofence,deletegeofence,getgeofencelist,getgeofencedetails,getallgeofencealerts}`.
- Observed: TCU packets clearly outside a 100 m fence, yet `getallgeofencealerts` returned no alerts → server engine never generated them. Recreating the fence or TVS support is the only route.
- `/api/Notification/NotificationList` → 404. FCM token registered only at OTP login (`verifyOTPRequest.androidToken`).
- App bugs: non-200 `statusCode` ignored, FCM payload parsed outside try (drops alert), channel importance DEFAULT, `parseInt(user_vehicle_id)` uncaught.
- Anti-theft: no app-side configuration (`TheftInput` never built), no remote immobilize.

### Weather
- `/api/weather` returns `{"statusCode":0}` for Nepal → iQube (U347 path) sends `"null null⁰ null"`. NTorq XP gets a hard-coded `"Cloudy 18⁰C"` once per connect (`isIBApp()` hard-coded true skips the fetch).

### NTorq XP
- `LocationTracker.startLocationUpdates` gated on `GoogleApiClient.isConnected()` right after async `connect()` → updates never start; every ride CSV row has the same lat/lng.
- `DBHelper.updateN251BleRideStatusForpause` deletes the ride row on offline/API error. Paused rides resumed without age check → merged into the next day's ride.
- Swiping the app away ends the ride service (`onTaskRemoved` → close, `START_NOT_STICKY`). Not patched.

### Privacy / security
- Play build logs the bearer JWT, email and user id to logcat via `System.out`, and dumps all phone notifications ("NotificationTest") — any app with log access or a bug report gets your token.

## Cluster BLE protocol (iQube, mostly static analysis)

Sender: `ble/feature/datasender/IQubeDataSenderToCluster` → `BleEngine.writeCharacteristicToCentral`. 20-byte frames, terminated `FF`. Checksum when used: byte 18 = 255 − (sum mod 256). XOR 0xEB only on U467 family.

Writes:
- `5A 4E` / `5B 4F` navigation (1 s timer; `BleNavigationSendData.byteArrayForNavigation`). Icon ids = HERE `NavInstructionsU546` table (0 left, 10 right, 57 continue, 22 arrive, 27–40 roundabouts) — vehicleTypeId 106 isn't in the app's BikeType enum so the app falls back to the wrong table.
- `4A` phone status every 2 s: [2] signal<<4 | battery ceil(pct/25), [3] 0x78, [4] temp+127, [5] OS=2, [6–9] 12 h clock, [10] missed calls, [11] network type.
- `43` caller (≤16, resent 500 ms) · `53` SMS sender (≤17, checksum) · `9C` social counts ([2] WhatsApp msgs<<3|chats, [3] FB, [4] IG) · `5C` location text · `5D` weather (new SKU: [2] icon, [3–4] `00 50`, text) · `52` rider name · `60–63` music · `5A F1` brightness/trip/date.

Inbound: `5A10` speed/odo/mode/ignition/trip · `5A13` SoC % + range · `5A19` trips A/B · `5A6D` cluster SKU · `5A9D` firmware version.

Location name: `BleEngine.sendLocationToCluster` → `BluetoothUtil.getAddressFromLatLng` (Android Geocoder, online only) → subLocality/locality truncated ~16 chars → `5B 5C`.

### NTorq XP (vehicleTypeId "9", old N251 stack)
`N251HelperService` → `N109U347N251DataSenderToCluster`; nav from `HereMapNavigationFragment.v()` every 800 ms via `BaseConnectHelperService.writeNavigationData`.
Frame 1 `5A 4E` dist(2 BE) eta-min(2) remain(3) picto … status (1 ≤1 km, 3/4 L/R <65 m, 5 straight >1 km, 6 no GPS, 7 no data, 8 arrived); frame 2 `5B 4F` ASCII ≤17.
Picto table (`Utils.maneuvarInstruction`): straight 7, quite L/R 0/3, heavy 1/4, light 2/5, U-turn 6/41, arrive 8, roundabout 1–3 = 71/70/69, LH roundabout 65/66/67, fork 15/16; keep-left/right and roundabout exits 4–12 → 0.

## MQTT packet fields seen
`latitude, longitude, heading, speed, speed_can, odometer_can, can_modified, soc, ign, driving_mode, datetime_pkt (UTC), tcu_connection_status, cellbar`. `driving_mode` "1" = Eco.
