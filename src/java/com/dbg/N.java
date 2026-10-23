package com.dbg;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import com.tvs.ble.feature.handler.BleEngine;
import com.tvs.ble.feature.navigation.BleNavigationSendData;
import com.tvselectric.mapnavigation.utils.NavInstructionsU546;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Google Maps turn-by-turn notification -> iQube cluster nav frames, via the same
// builder + BleEngine call the app's own HERE navigation uses (BleNavigationDataSourceImp IQube branch).
// Hooked in NotificationListenerService.onNotificationPosted / onNotificationRemoved.
public final class N {
    static final String MAPS = "com.google.android.apps.maps";
    static BleEngine eng;
    static Object frame; // kotlin.Pair from byteArrayForNavigation, resent every 1 s
    static java.util.Timer timer;

    public static void p(BleEngine e, StatusBarNotification sbn) {
        try {
            if (sbn == null || !MAPS.equals(sbn.getPackageName())) return;
            Notification n = sbn.getNotification();
            if (!"navigation".equals(n.category)) return;
            com.tvsm.connect.bluetooth.bluetoothlegatt.BaseConnectHelperService h = xpSvc();
            if (h == null && (e == null || test(e))) return;
            Bundle x = n.extras;
            String title = s(x.getCharSequence("android.title")), text = s(x.getCharSequence("android.text"));
            String sub = s(x.getCharSequence("android.subText"));
            String act = action(text);
            // iQube cluster uses the U546 0-63 icon set; NavInstructions ids (44=right) showed wrong icons on ride 2026-10-22
            int picto = NavInstructionsU546.valueOf(act).getPictogramId();
            int d = meters(title), left = meters(sub);
            // cluster time field = time to next turn: route avg speed (left/sec) applied to distance d
            long sec = left > 0 ? 60L * (long) Math.ceil(minutes(sub) * (double) Math.max(d, 0) / left) : 0; // whole min, rounded up (cluster shows sec/60)
            String lbl = label(text);
            if (h != null) { // NTorq XP (old N251 stack)
                byte[][] xf = xp(act, d, left, minutes(sub), lbl);
                F.a("GMAPS_NAV xp " + title + " | " + text + " | " + sub + " -> " + act + " picto=" + xf[0][9] + " st=" + xf[0][14] + " d=" + d + " lbl=" + lbl);
                synchronized (N.class) { xps = h; xpf = xf; frame = null; send(); timer(); }
                return;
            }
            F.a("GMAPS_NAV " + title + " | " + text + " | " + sub + " -> " + act + " picto=" + picto + " d=" + d + " left=" + left + " sec=" + sec + " lbl=" + lbl);
            Object f = BleNavigationSendData.Companion.byteArrayForNavigation(Math.max(d, 0), 0L, Math.max(left, 0), sec, 0, lbl, picto, null);
            synchronized (N.class) {
                eng = e;
                frame = f;
                xpf = null;
                send();
                timer();
            }
        } catch (Throwable t) {
            F.a("GMAPS_NAV err " + t);
        }
    }

    // Icon survey: if TEST file exists when Maps nav starts, show cluster ids 0..63, 3 s each
    // (distance field = id, text "ICON n"), then delete the file and resume normal nav.
    static final java.io.File TEST = new java.io.File("/sdcard/Android/data/com.tvsm.connect.nepdb/files/picto_test");
    static volatile boolean testing;

    static boolean test(final BleEngine e) {
        if (testing) return true;
        if (!TEST.exists()) return false;
        testing = true;
        TEST.delete();
        new Thread("gmaps-picto-test") { public void run() {
            try {
                for (int i = 0; i < 64; i++) {
                    Object f = BleNavigationSendData.Companion.byteArrayForNavigation(i, 0L, 0, 0L, 0, "ICON " + i, i, null);
                    F.a("GMAPS_NAV test picto=" + i);
                    for (int k = 0; k < 3; k++) {
                        synchronized (N.class) { eng = e; frame = f; send(); }
                        Thread.sleep(1000);
                    }
                }
                texts(e);
            } catch (Throwable t) { F.a("GMAPS_NAV test err " + t); }
            testing = false;
            F.a("GMAPS_NAV test done");
        } }.start();
        return true;
    }

    // Display text survey (runs after icons): each step 8 s, resent 1 s; nav text names the step.
    static void texts(BleEngine e) throws InterruptedException {
        Object[][] steps = {
            {"T1 CALLER", new byte[][]{txt(0x43, "CALLER TEST")}, false},
            {"T2 SMS", new byte[][]{txt(0x53, "SMS TEST")}, true},
            {"T3 SOCIAL", new byte[][]{raw(0x9C, (5 << 3) | 2, 0x33, 4)}, true},
            {"T4 LOCATION", new byte[][]{txt(0x5C, "LOCATION TEST")}, false},
            {"T5 WEATHER", new byte[][]{txt(0x5D, "WEATHER TEST")}, false},
            {"T6 RIDER", new byte[][]{txt(0x52, "RIDER TEST")}, false},
            {"T7 MUSIC", new byte[][]{raw(0x60, 0, 0, 0, 0, 0, 50), txt(0x62, "TRACK TEST"), txt(0x63, "ARTIST TEST"), txt(0x61, "ALBUM TEST")}, true},
        };
        for (int i = 0; i < steps.length; i++) {
            String name = (String) steps[i][0];
            byte[][] fs = (byte[][]) steps[i][1];
            boolean ck = (Boolean) steps[i][2];
            Object nav = BleNavigationSendData.Companion.byteArrayForNavigation(i + 1, 0L, 0, 0L, 0, name, 57, null);
            F.a("GMAPS_NAV test text " + name);
            for (int k = 0; k < 8; k++) {
                synchronized (N.class) {
                    eng = e; frame = nav; send();
                    for (byte[] f : fs) e.writeCharacteristicToCentral(f.clone(), ck);
                }
                Thread.sleep(1000);
            }
        }
    }

    // 5B <id> <ascii text, zero padded> .. FF (20 bytes; text <=17)
    static byte[] txt(int id, String t) {
        byte[] f = new byte[20];
        f[0] = 0x5B; f[1] = (byte) id; f[19] = (byte) 0xFF;
        byte[] b = t.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(b, 0, f, 2, Math.min(b.length, 17));
        return f;
    }

    static byte[] raw(int id, int... v) {
        byte[] f = new byte[20];
        f[0] = 0x5B; f[1] = (byte) id; f[19] = (byte) 0xFF;
        for (int i = 0; i < v.length; i++) f[2 + i] = (byte) v[i];
        return f;
    }

    // Weather: TVS /api/weather returns empty data for Nepal -> app sends "null null⁰ null".
    // Hooked at top of BluetoothUtil.createClimate: swap broken text for Open-Meteo current weather at
    // scooter's last MQTT position (lastpos.txt, rounded ~1 km; user OK'd 2026-10-22). Non-blocking: cache + bg refresh.
    static final String POS = "/sdcard/Android/data/com.tvsm.connect.nepdb/files/lastpos.txt";
    static volatile String wx;
    static volatile long wxAt;

    public static String w(String s) {
        if (s != null && !s.contains("null") && !s.equals("-")) return s;
        if (System.currentTimeMillis() - wxAt > 15 * 60_000L) {
            wxAt = System.currentTimeMillis();
            new Thread("gmaps-wx") { public void run() {
                try {
                    String[] p = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(POS))).split(",");
                    String u = String.format(java.util.Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code",
                            Double.parseDouble(p[0]), Double.parseDouble(p[1]));
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
                    c.setConnectTimeout(10000); c.setReadTimeout(10000);
                    java.util.Scanner sc = new java.util.Scanner(c.getInputStream(), "UTF-8").useDelimiter("\\A");
                    wx = weather(sc.hasNext() ? sc.next() : "");
                    F.a("GMAPS_NAV wx " + wx);
                    if (wx != null) java.nio.file.Files.write(java.nio.file.Paths.get(WXF), wx.getBytes("UTF-8"));
                } catch (Throwable t) { wxAt = System.currentTimeMillis() - 14 * 60_000L; F.a("GMAPS_NAV wx err " + t); } // retry in ~1 min
            } }.start();
        }
        return wx != null ? wx : s;
    }

    // NTorq XP (old N251 stack): app sends hardcoded "Cloudy 18⁰C" once per connect via old BluetoothUtil.createClimate.
    // Always swap; process is often fresh at connect, so fall back to last fetch saved on disk (<3 h old).
    static final String WXF = "/sdcard/Android/data/com.tvsm.connect.nepdb/files/wx.txt";

    public static String w2(String s) {
        String r = w(null);
        if (r != null) return r;
        try {
            java.io.File f = new java.io.File(WXF);
            if (System.currentTimeMillis() - f.lastModified() < 3 * 3600_000L) return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
        } catch (Throwable t) { }
        return s;
    }

    // NTorq XP rides: a paused ride is resumed at next connect with no age check (N251ClusterDataReceiver.G0),
    // so yesterday's ride merged into today's and uploaded a day late. Called at top of G0 and UniDashboardActivity.onResume:
    // paused >1 h (app's own "continue within 1 hr" rule) -> unpause (DBHelper delete removed) + upload as finished ride.
    public static void pz(android.content.Context c) {
        try {
            com.tvsm.connect.DBHelper db = com.tvsm.connect.DBHelper.getInstance();
            com.tvsm.connect.bikedashboard.n251.model.N251BleRideDataModel m = db.getN251BleLastPausedRide();
            if (m == null || System.currentTimeMillis() - m.getTravelEndDateTime() < 3600_000L) return;
            db.updateN251BleRideStatusForpause(m.getTravelId());
            new com.tvsm.connect.receiveres.SyncRidesForBikes(c).callApi();
            F.a("RIDE stale paused ride closed + sync " + m.getTravelId());
        } catch (Throwable t) { F.a("RIDE pz err " + t); }
    }

    // XP ride detail (U458RideDetailFragment.onCreateView) only reads/downloads the route CSV when travelFileName != null.
    // Log what the server gave; if name missing but link present, use the link's file name so the route gets downloaded.
    public static String rd(com.tvsm.connect.bikedashboard.n251.model.N251BleRideDataModel m) {
        String n = m.getTravelFileName(), l = m.getTravelStatsDownloadLink();
        String r = n == null || n.isEmpty() ? csvName(l) : n;
        F.a("RIDE detail id=" + m.getTravelId() + " file=" + n + " link=" + (l == null ? null : l.replaceAll("\\?.*", "?..")) + " -> " + r);
        return r;
    }

    static String csvName(String l) {
        if (l == null || l.isEmpty()) return null;
        String p = l.replaceAll("[?#].*", "");
        p = p.substring(p.lastIndexOf('/') + 1);
        return p.isEmpty() ? null : p;
    }

    // Open-Meteo JSON -> app's own format "Cloudy 18⁰ C". First word must be one BluetoothUtil.getWeatherIcon
    // knows (clear/cloudy/foggy/rain/snow), else cluster gets icon 12 (unknown) - "Fair" did that on 2026-10-23.
    // New-protocol frame fits 14 text bytes (⁰ = 3 bytes); drop unit if longer.
    static String weather(String json) {
        Matcher t = Pattern.compile("\"temperature_2m\":(-?[\\d.]+)").matcher(json), c = Pattern.compile("\"weather_code\":(\\d+)").matcher(json);
        if (!t.find() || !c.find()) return null;
        int w = Integer.parseInt(c.group(1));
        String d = w <= 1 ? "Clear" : w <= 3 ? "Cloudy" : w <= 48 ? "Foggy" : (w >= 71 && w <= 77) || w == 85 || w == 86 ? "Snow" : "Rain";
        String r = d + " " + Math.round(Double.parseDouble(t.group(1))) + "\u2070 C";
        return r.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 14 ? r.substring(0, r.length() - 2) : r;
    }

    public static void r(StatusBarNotification sbn) {
        if (sbn == null || !MAPS.equals(sbn.getPackageName()) || !"navigation".equals(sbn.getNotification().category)) return;
        synchronized (N.class) {
            if (timer != null) timer.cancel();
            timer = null;
            frame = null;
            xpf = null;
        }
        F.a("GMAPS_NAV stopped");
    }

    static void timer() {
        if (timer != null) return;
        timer = new java.util.Timer("gmaps-nav", true);
        timer.schedule(new java.util.TimerTask() { public void run() { synchronized (N.class) { send(); } } }, 1000, 1000);
    }

    // NTorq XP: frames written through the old stack's helper service, like HereMapNavigationFragment.O().
    static com.tvsm.connect.bluetooth.bluetoothlegatt.BaseConnectHelperService xps;
    static byte[][] xpf;

    static com.tvsm.connect.bluetooth.bluetoothlegatt.BaseConnectHelperService xpSvc() {
        try {
            com.tvsm.connect.bluetooth.bluetoothlegatt.BaseConnectHelperService h = com.tvsm.connect.TVSApplication.getInstance().getBikeConnectionHelperService();
            return h != null && h.isClusterConnected() ? h : null;
        } catch (Throwable t) { return null; }
    }

    // HERE ManeuverAction name -> XP pictogram (Utils.ManeuverInst table; ids the app never fills fall back to nearest).
    static int xpPicto(String a) {
        if (a.startsWith("LEFT_ROUNDABOUT_EXIT")) { int n = Integer.parseInt(a.substring(20)); return n == 1 ? 65 : n == 2 ? 66 : 67; }
        switch (a) {
            case "LEFT_TURN": return 0; case "SHARP_LEFT_TURN": return 1; case "SLIGHT_LEFT_TURN": return 2;
            case "RIGHT_TURN": return 3; case "SHARP_RIGHT_TURN": return 4; case "SLIGHT_RIGHT_TURN": return 5;
            case "LEFT_U_TURN": return 6; case "RIGHT_U_TURN": return 41; case "ARRIVE": return 8;
            case "LEFT_FORK": return 15; case "RIGHT_FORK": return 16; case "LEFT_RAMP": return 34; case "RIGHT_RAMP": return 37;
            case "LEFT_ROUNDABOUT_ENTER": return 67;
            default: return 7;
        }
    }

    // Port of HereMapNavigationFragment.v(): 5A 4E dist(2) eta-min(2) remain(3) picto lines 01 00 00 status 0 000000 FF,
    // then 5B 4F text(<=17) FF. status: 1 <=1 km, 3/4 left/right <65 m, 5 straight >1 km (picto forced 7), 8 arrived.
    static byte[][] xp(String act, int d, int left, int min, String t) {
        d = Math.max(d, 0);
        int picto = xpPicto(act), st;
        if (act.equals("ARRIVE") && d < 30) { st = 8; d = 0; t = "Arrived"; }
        else if (d < 65) st = act.contains("LEFT") ? 3 : act.contains("RIGHT") ? 4 : picto == 7 ? 5 : 0;
        else if (d <= 1000) st = 1;
        else { st = 5; picto = 7; }
        d = Math.min(d, 32767); min = Math.min(Math.max(min, 0), 65535); left = Math.max(left, 0);
        byte[] f = new byte[20];
        f[0] = 0x5A; f[1] = 0x4E;
        f[2] = (byte) (d >> 8); f[3] = (byte) d; f[4] = (byte) (min >> 8); f[5] = (byte) min;
        f[6] = (byte) (left >> 16); f[7] = (byte) (left >> 8); f[8] = (byte) left;
        f[9] = (byte) picto; f[10] = (byte) (t.length() > 17 ? 2 : 1); f[11] = 1; f[14] = (byte) st; f[19] = (byte) 0xFF;
        return new byte[][]{f, txt(0x4F, t)};
    }

    static void send() {
        if (xpf != null) {
            try { xps.writeNavigationData(xpf[0]); Thread.sleep(100); xps.writeNavigationData(xpf[1]); } catch (Throwable t) { F.a("GMAPS_NAV xp send err " + t); }
            return;
        }
        if (frame == null) return;
        try { eng.sendNavigationData((kotlin.Pair) frame, 0, 0, ""); } catch (Throwable t) { F.a("GMAPS_NAV send err " + t); }
    }

    static String s(CharSequence c) { return c == null ? "" : c.toString(); }

    // "250 m", "1.2 km", "8 min · 1.7 km · 10:24 AM ETA" -> first distance in meters, else -1
    static int meters(String s) {
        Matcher m = Pattern.compile("([\\d.,]+)\\s*(km|m)\\b").matcher(s);
        if (!m.find()) return -1;
        double v = Double.parseDouble(m.group(1).replace(",", ""));
        return (int) Math.round(m.group(2).equals("km") ? v * 1000 : v);
    }

    // "1 hr 5 min · ..." -> 65; no time -> 0
    static int minutes(String s) {
        Matcher h = Pattern.compile("(\\d+)\\s*(hr|h)\\b").matcher(s), m = Pattern.compile("(\\d+)\\s*min").matcher(s);
        return (h.find() ? Integer.parseInt(h.group(1)) * 60 : 0) + (m.find() ? Integer.parseInt(m.group(1)) : 0);
    }

    // Split "Turn left onto Ring Road" into verb ("turn left") and road ("Ring Road").
    static final Pattern SPLIT = Pattern.compile("\\s+(onto|toward|towards|on|at|to)\\s+", Pattern.CASE_INSENSITIVE);

    // Google instruction text -> HERE ManeuverAction name (both pictogram enums use these names).
    // Nepal drives on the left: roundabouts are clockwise (HERE LEFT_ROUNDABOUT_*), U-turns swing right.
    static String action(String text) {
        String t = text.toLowerCase();
        Matcher sp = SPLIT.matcher(t);
        String v = sp.find() ? t.substring(0, sp.start()) : t;
        boolean l = v.contains("left"), r = v.contains("right");
        if (t.contains("arrive") || t.contains("destination")) return "ARRIVE";
        if (v.contains("u-turn") || v.contains("u turn")) return "RIGHT_U_TURN";
        if (t.contains("roundabout")) {
            Matcher m = Pattern.compile("(\\d+)(st|nd|rd|th) exit").matcher(t);
            if (!m.find()) return "LEFT_ROUNDABOUT_ENTER";
            return "LEFT_ROUNDABOUT_EXIT" + Math.min(Math.max(Integer.parseInt(m.group(1)), 1), 12);
        }
        if (v.contains("sharp")) return l ? "SHARP_LEFT_TURN" : r ? "SHARP_RIGHT_TURN" : "CONTINUE_ON";
        if (v.contains("slight") || v.contains("keep") || v.contains("bear")) return l ? "SLIGHT_LEFT_TURN" : r ? "SLIGHT_RIGHT_TURN" : "CONTINUE_ON";
        if (v.contains("fork")) return l ? "LEFT_FORK" : r ? "RIGHT_FORK" : "MIDDLE_FORK";
        if (v.contains("ramp") || v.contains("exit")) return t.contains("left") ? "LEFT_RAMP" : t.contains("right") ? "RIGHT_RAMP" : "CONTINUE_ON";
        if (l) return "LEFT_TURN";
        if (r) return "RIGHT_TURN";
        return "CONTINUE_ON";
    }

    // Cluster text (<=17 chars, frame is 20 bytes): road name if any, else instruction. ASCII only so
    // the 17-char cut can't overflow the fixed frame with multi-byte UTF-8.
    static String label(String text) {
        Matcher sp = SPLIT.matcher(text);
        String s = sp.find() ? text.substring(sp.end()) : text;
        s = s.replaceAll("[^\\x20-\\x7E]", "").trim();
        return s.isEmpty() ? "Navigate" : s;
    }

    public static void main(String[] a) { // local check: java -cp out com.dbg.N
        String[][] cases = {
            {"Head southwest", "CONTINUE_ON"}, {"Turn left onto Ring Road", "LEFT_TURN"},
            {"Turn right onto Left Bank Rd", "RIGHT_TURN"}, {"Slight right toward Araniko Hwy", "SLIGHT_RIGHT_TURN"},
            {"Keep left at the fork", "SLIGHT_LEFT_TURN"}, {"Sharp left", "SHARP_LEFT_TURN"},
            {"At the roundabout, take the 2nd exit onto Kanti Path", "LEFT_ROUNDABOUT_EXIT2"},
            {"Enter the roundabout", "LEFT_ROUNDABOUT_ENTER"}, {"Make a U-turn", "RIGHT_U_TURN"},
            {"Arrive at Bhaktapur Durbar Square", "ARRIVE"}, {"Your destination is on the left", "ARRIVE"},
            {"Take the ramp on the left", "LEFT_RAMP"},
        };
        for (String[] c : cases) if (!action(c[0]).equals(c[1])) throw new AssertionError(c[0] + " -> " + action(c[0]));
        if (meters("0 m") != 0 || meters("250 m") != 250 || meters("1.2 km") != 1200 || meters("Rerouting") != -1) throw new AssertionError("meters");
        if (meters("8 min · 1.7 km · 10:24 AM ETA") != 1700 || minutes("8 min · 1.7 km · 10:24 AM ETA") != 8) throw new AssertionError("sub");
        if (minutes("1 hr 5 min · 40 km · 11:30 AM ETA") != 65) throw new AssertionError("hr");
        if (!label("Turn left onto Ring Road").equals("Ring Road") || !label("Head southwest").equals("Head southwest")) throw new AssertionError("label");
        if (!label("Turn right onto काठमाडौं").equals("Navigate")) throw new AssertionError(label("Turn right onto काठमाडौं"));
        if (txt(0x43, "CALLER TEST")[2] != 'C' || txt(0x43, "CALLER TEST")[19] != (byte) 0xFF || raw(0x9C, 42)[2] != 42) throw new AssertionError("frame");
        String j = "{\"current_units\":{\"temperature_2m\":\"°C\",\"weather_code\":\"wmo code\"},\"current\":{\"time\":\"2026-10-22T19:00\",\"temperature_2m\":17.6,\"weather_code\":3}}";
        if (!"Cloudy 18\u2070 C".equals(weather(j)) || !"Cloudy -10\u2070".equals(weather("\"temperature_2m\":-10.2,\"weather_code\":2"))) throw new AssertionError(weather(j));
        if (weather("{}") != null || !"Sunny 20C".equals(w("Sunny 20C"))) throw new AssertionError("wx");
        byte[][] x = xp("RIGHT_TURN", 300, 1700, 8, "Ring Road");
        if (x[0].length != 20 || x[0][0] != 0x5A || x[0][1] != 0x4E || x[0][2] != 1 || x[0][3] != 44 || x[0][5] != 8 || x[0][7] != 6 || x[0][8] != (byte) 0xA4
            || x[0][9] != 3 || x[0][14] != 1 || x[0][19] != (byte) 0xFF || x[1][1] != 0x4F || x[1][2] != 'R') throw new AssertionError("xp");
        if (xp("LEFT_TURN", 40, 0, 0, "x")[0][14] != 3 || xp("CONTINUE_ON", 2000, 0, 0, "x")[0][14] != 5 || xp("ARRIVE", 10, 0, 0, "x")[0][14] != 8
            || xpPicto("LEFT_ROUNDABOUT_EXIT2") != 66 || xpPicto("LEFT_ROUNDABOUT_EXIT7") != 67) throw new AssertionError("xp2");
        if (!"R_1_2_3.csv".equals(csvName("https://x.y/a/b/R_1_2_3.csv?sig=1")) || csvName(null) != null) throw new AssertionError("csv");
        System.out.println("N OK");
    }
}
