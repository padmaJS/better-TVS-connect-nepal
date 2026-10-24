package com.dbg;

import java.io.BufferedReader;
import java.io.FileReader;

// Offline nearest-place lookup for the cluster location name (used when Geocoder has no internet).
// places.txt: "lat\tlng\tname" per line, built from GeoNames NP.zip (see tools/geo/build_places.sh).
public final class G {
    static final String PATH = System.getProperty("geo.path", "/sdcard/Android/data/com.tvsm.connect.nepdb/files/places.txt");
    static final double MAX_KM = 15;
    static double[] la, lo;
    static String[] nm;

    static synchronized void load() {
        if (nm != null) return;
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(PATH))) {
            for (String s; (s = r.readLine()) != null; ) {
                String[] p = s.split("\t");
                if (p.length == 3) rows.add(p);
            }
        } catch (Exception e) {
            // missing/unreadable file -> empty list, lookups return null
        }
        int n = rows.size();
        la = new double[n]; lo = new double[n]; nm = new String[n];
        for (int i = 0; i < n; i++) {
            String[] p = rows.get(i);
            try { la[i] = Double.parseDouble(p[0]); lo[i] = Double.parseDouble(p[1]); }
            catch (NumberFormatException e) { la[i] = 999; }
            nm[i] = p[2];
        }
    }

    // nearest place name within MAX_KM, else null. Linear scan (~10k rows), fine at 1 call/s.
    public static String n(double lat, double lng) {
        load();
        double k = Math.cos(Math.toRadians(lat)), best = Double.MAX_VALUE;
        int bi = -1;
        for (int i = 0; i < nm.length; i++) {
            double dy = la[i] - lat, dx = (lo[i] - lng) * k, d = dx * dx + dy * dy;
            if (d < best) { best = d; bi = i; }
        }
        String res = (bi >= 0 && Math.sqrt(best) * 111.2 <= MAX_KM) ? nm[bi] : null;
        F.a("OFFLINE_GEO " + lat + "," + lng + " -> " + res);
        if (res != null) { cLa = lat; cLo = lng; cNm = res; }
        return res;
    }

    // Riding triggers ~700 lookups/min. Reuse last name within 200 m; skip the (always failing) Geocoder
    // for 10 min after it throws. null -> caller runs Geocoder as before.
    static double cLa = 999, cLo;
    static String cNm;
    static long geoOff;

    public static synchronized String c(double lat, double lng) {
        double dy = cLa - lat, dx = (cLo - lng) * Math.cos(Math.toRadians(lat));
        if (cNm != null && Math.sqrt(dx * dx + dy * dy) * 111.2 < 0.2) return cNm;
        return System.currentTimeMillis() < geoOff ? n(lat, lng) : null;
    }

    // Geocoder threw: offline name, Geocoder off for 10 min.
    public static synchronized String e(double lat, double lng) {
        geoOff = System.currentTimeMillis() + 600_000;
        return n(lat, lng);
    }

    // Server ride times are IST (UTC+5:30); re-express in phone tz (Nepal +5:45), same format. Unparseable -> unchanged.
    public static String t(String fmt, String s) {
        if (s == null) return null;
        for (String f : new String[]{fmt, "yyyy-MM-dd HH:mm:ss"}) {
            if (f == null) continue;
            try {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(f, java.util.Locale.ENGLISH);
                in.setLenient(false);
                in.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                java.util.Date d = in.parse(s.trim());
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat(f, java.util.Locale.ENGLISH);
                out.setTimeZone(java.util.TimeZone.getDefault());
                return out.format(d);
            } catch (Exception e) { }
        }
        return s;
    }

    // Drop-in replacements for the app's DateTimeUtils calls on ride screens (input shifted IST -> local first).
    public static String a1(com.tvselectric.vehiclestats.utils.DateTimeUtils u, String cur, String req, String s) {
        return u.changeDateTimeFormatInIST(cur, req, t(cur, s));
    }

    public static String a2(com.tvselectric.vehiclestats.utils.DateTimeUtils u, String cur, String s) {
        return u.changeDateTimeFormatInISTWithSuffix(cur, t(cur, s));
    }

    // FCM debug logging (token value never logged).
    public static void fm(com.google.firebase.messaging.RemoteMessage m) {
        try {
            com.google.firebase.messaging.RemoteMessage.Notification n = m.getNotification();
            F.a("FCM_MSG from=" + m.getFrom() + " id=" + m.getMessageId() + " data=" + m.getData()
                    + (n == null ? "" : " title=" + n.getTitle() + " body=" + n.getBody()));
        } catch (Throwable e) { F.a("FCM_MSG log err " + e); }
    }

    public static void ft(com.google.android.gms.tasks.Task<?> t) {
        try {
            Object r = t.isSuccessful() ? t.getResult() : null;
            F.a("FCM_TOKEN ok=" + t.isSuccessful() + " len=" + (r == null ? 0 : r.toString().length()) + " err=" + t.getException());
        } catch (Throwable e) { F.a("FCM_TOKEN log err " + e); }
    }

    static boolean fqDone;

    // once per process: does this (cloned) package get an FCM token at all?
    public static void fq() {
        if (fqDone) return;
        fqDone = true;
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<String>() {
                        public void onComplete(com.google.android.gms.tasks.Task<String> t) { ft(t); }
                    });
        } catch (Throwable e) { F.a("FCM_TOKEN getToken threw " + e); }
    }

    public static void fn(String tok) {
        F.a("FCM_NEW_TOKEN len=" + (tok == null ? 0 : tok.length()));
    }


    // "Location as of": TCU repeats its last fix until GPS locks (minutes after ignition on), so track when lat/lng last changed.
    // State "lat,lng,changedPkt,lastPkt" persisted next to places.txt so it survives process restarts. Pkt times are UTC "yyyy-MM-dd HH:mm:ss".
    static final String POS = System.getProperty("pos.path", "/sdcard/Android/data/com.tvsm.connect.nepdb/files/lastpos.txt");
    static String[] pos;

    public static synchronized void lp(com.tvselectric.mapnavigation.model.MQTTVehicleDataResponse r) {
        try { p(r.getDatetimePkt(), r.getLatitude(), r.getLongitude()); } catch (Throwable e) { F.a("LOC_ASOF err " + e); }
        ride = new String[]{r.getDatetimePkt(), r.getIgnitionStatus(), String.valueOf(r.getSpeed()), r.getDrivingMode()};
    }

    static void p(String pkt, double lat, double lng) throws Exception {
        if (pkt == null || pkt.length() != 19) return;
        if (pos == null) {
            pos = new String[4];
            try (BufferedReader r = new BufferedReader(new FileReader(POS))) {
                String[] a = r.readLine().split(",");
                if (a.length == 4) pos = a;
            } catch (Exception e) { }
        }
        if (pos[3] != null && pkt.compareTo(pos[3]) <= 0) return; // duplicate/out-of-order packet
        String la = String.valueOf(lat), lo = String.valueOf(lng);
        if (!la.equals(pos[0]) || !lo.equals(pos[1]) || pos[2] == null) {
            pos = new String[]{la, lo, pkt, pkt};
            F.a("LOC_ASOF moved " + pkt);
        } else pos[3] = pkt;
        try (java.io.FileWriter w = new java.io.FileWriter(POS)) { w.write(String.join(",", pos)); }
    }

    static String[] ride; // pkt, ign, GPS speed, mode of the latest packet

    // Vehicle status line + " · 25 km/h · Eco" while ignition on and packet < 2 min old (GPS speed: speed_can is only refreshed every ~2 min, see can_modified).
    public static synchronized String ls(String label) {
        try {
            if (ride == null || !"1".equals(ride[1])) return label;
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ENGLISH);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            if (System.currentTimeMillis() - in.parse(ride[0]).getTime() > 120000) return label;
            String m = "1".equals(ride[3]) ? " · Eco" : "2".equals(ride[3]) ? " · Power" : "0".equals(ride[3]) ? "" : " · Mode " + ride[3]; // codes from app's DrivingMode (1 eco, 2 pwr); 0 = none/standstill
            return label + " · " + Math.round(Double.parseDouble(ride[2])) + " km/h" + m;
        } catch (Exception e) { return label; }
    }

    // Card title is "%2$s of %1$s" (resource, not rebuilt): "Location as of 2:13 PM of NAME" -> "NAME · as of 2:13 PM".
    public static String lt(String t) {
        String p = "Location as of ";
        if (t == null || !t.startsWith(p)) return t;
        int k = t.indexOf(" of ", p.length());
        return k < 0 ? t : t.substring(k + 4) + " · as of " + t.substring(p.length(), k);
    }

    public static synchronized String ll(String label) {
        if (pos == null || pos[2] == null) return label;
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ENGLISH);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = in.parse(pos[2]);
            String day = new java.text.SimpleDateFormat("yyyyMMdd").format(d);
            boolean today = day.equals(new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()));
            return "Location as of " + new java.text.SimpleDateFormat(today ? "h:mm a" : "MMM d, h:mm a", java.util.Locale.ENGLISH).format(d);
        } catch (Exception e) { return label; }
    }

    public static void main(String[] a) {
        if (a.length == 0) { // local check: java -Dpos.path=/tmp/x -cp out com.dbg.G
            try {
                p("2026-10-21 02:11:28", 27.70000, 85.30000);
                p("2026-10-21 02:13:50", 27.70000, 85.30000);   // GPS still frozen
                p("2026-10-21 02:12:00", 27.1, 85.1);           // out of order, ignored
                if (!"2026-10-21 02:11:28".equals(pos[2])) throw new AssertionError(pos[2]);
                p("2026-10-21 02:13:58", 27.70100, 85.30100);      // moved
                if (!"2026-10-21 02:13:58".equals(pos[2])) throw new AssertionError(pos[2]);
                pos = null; p("2026-10-21 02:14:03", 27.70100, 85.30100); // reload from file
                if (!"2026-10-21 02:13:58".equals(pos[2])) throw new AssertionError(pos[2]);
                System.out.println(ll("Live Location") + " OK");
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                ride = new String[]{f.format(new java.util.Date()), "1", "24.7109375", "1"};
                if (!ls("On Ride").equals("On Ride · 25 km/h · Eco")) throw new AssertionError(ls("On Ride"));
                ride[3] = "2"; if (!ls("On Ride").equals("On Ride · 25 km/h · Power")) throw new AssertionError(ls("On Ride"));
                ride[3] = "0"; if (!ls("On Ride").equals("On Ride · 25 km/h")) throw new AssertionError(ls("On Ride"));
                ride[0] = "2026-10-21 02:00:00";
                if (!ls("Locked").equals("Locked")) throw new AssertionError(ls("Locked"));
                System.out.println(ls("On Ride") + " OK");
                if (!lt("Location as of Oct 20, 8:11 PM of MY SCOOTER").equals("MY SCOOTER · as of Oct 20, 8:11 PM")) throw new AssertionError();
                if (!lt("Live Location of X").equals("Live Location of X")) throw new AssertionError();
                System.out.println(lt("Location as of 2:13 PM of MY SCOOTER") + " OK");
                if (c(27.7172, 85.3240) != null) throw new AssertionError("geocoder should run first");
                String b = e(27.7172, 85.3240);
                if (b == null || !b.equals(c(27.7180, 85.3245))) throw new AssertionError("cache within 200 m");
                cNm = null; if (c(27.70, 85.32) == null) throw new AssertionError("offline while geocoder off");
                geoOff = 0; cLa = 999; cNm = null; if (c(27.70, 85.32) != null) throw new AssertionError();
                System.out.println("geo cache " + b + " OK");
            } catch (Exception e) { throw new RuntimeException(e); }
            return;
        }
        if (a.length == 1) { // local check: TZ=Asia/Kathmandu ... com.dbg.G "2026-10-20 20:25:23" -> 20:40:23
            System.out.println(t("yyyy-MM-dd HH:mm:ss", a[0]));
            System.out.println(t("MMM dd, yyyy HH:mm:ss", "Oct 20, 2026 20:25:23"));
            return;
        }
        // local check: java -Dgeo.path=places.txt -cp out com.dbg.G 27.6721 85.4281 -> Bhaktapur
        System.out.println(n(Double.parseDouble(a[0]), Double.parseDouble(a[1])));
    }
}
