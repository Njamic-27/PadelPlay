package hr.padel.termini;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns the termini into alarms. How long before a termin the reminders fire is a list of
 * minutes: the shared list from the settings, or the termin's own "rem" list when it has one.
 * A termin without a time is treated as a whole day: its reminders fire at 09:00.
 * Every change cancels and rebuilds all.
 */
final class Scheduler {
    static final String ACTION = "hr.padel.termini.REMIND";
    private static final String KEY_COUNT = "alarmCount";
    /** Longest allowed lead: 30 days. */
    private static final int MAX_LEAD = 30 * 1440;
    /** AlarmManager allows ~500 alarms per app; stay well below. */
    private static final int MAX_ALARMS = 400;

    private Scheduler() {}

    private static final class Reminder {
        final long at; final int nid; final String title; final String text;
        Reminder(long at, int nid, String title, String text) {
            this.at = at; this.nid = nid; this.title = title; this.text = text;
        }
    }

    private static Intent baseIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION);
        return i;
    }

    static synchronized void rescheduleAll(Context ctx) {
        Context c = ctx.getApplicationContext();
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        SharedPreferences p = c.getSharedPreferences(Storage.PREFS, Context.MODE_PRIVATE);

        // 1) cancel everything scheduled last time
        int prev = p.getInt(KEY_COUNT, 0);
        for (int i = 0; i < prev; i++) {
            PendingIntent old = PendingIntent.getBroadcast(c, i, baseIntent(c),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (old != null) { am.cancel(old); old.cancel(); }
        }

        // 2) schedule what is still in the future, soonest first
        long now = System.currentTimeMillis();
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        List<Reminder> all = build(Storage.load(c), Storage.loadReminders(c));
        Collections.sort(all, (a, b) -> Long.compare(a.at, b.at));
        int n = 0;
        for (Reminder r : all) {
            if (r.at <= now) continue;
            if (n >= MAX_ALARMS) break;
            Intent it = baseIntent(c);
            it.putExtra("nid", r.nid);
            it.putExtra("title", r.title);
            it.putExtra("text", r.text);
            PendingIntent pi = PendingIntent.getBroadcast(c, n, it,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            try {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi);
            } catch (SecurityException e) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi);
            }
            n++;
        }
        p.edit().putInt(KEY_COUNT, n).apply();
    }

    /** Cleans a list of leads coming from the page: positive, at most 30 days, no duplicates, longest first. */
    static String sanitizeLeads(String json) {
        List<Integer> l;
        try { l = parseLeads(new JSONArray(json)); } catch (Exception e) { l = new ArrayList<>(); }
        Collections.sort(l, Collections.reverseOrder());
        return new JSONArray(l).toString();
    }

    private static List<Integer> parseLeads(JSONArray a) {
        List<Integer> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            int m = a.optInt(i, -1);
            if (m > 0 && m <= MAX_LEAD && !out.contains(m)) out.add(m);
        }
        return out;
    }

    // ---------- texts ----------

    /** 90 -> "1 h 30 min", 1500 -> "1 dan 1 h" */
    private static String fmtLead(int m) {
        int d = m / 1440, h = (m % 1440) / 60, min = m % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d == 1 ? "1 dan" : d + " dana");
        if (h > 0) { if (sb.length() > 0) sb.append(' '); sb.append(h).append(" h"); }
        if (min > 0) { if (sb.length() > 0) sb.append(' '); sb.append(min).append(" min"); }
        return sb.toString();
    }

    private static String dayWord(int days) {
        switch (days) {
            case 0: return "danas";
            case 1: return "sutra";
            case 2: return "prekosutra";
            default: return "za " + days + " dana";
        }
    }

    /** "sutra u 19:30", "za 2 h, u 19:30", "za 1 dan 3 h, u 19:30" */
    private static String when(int lead, String time) {
        if (lead % 1440 == 0) return dayWord(lead / 1440) + " u " + time;
        return "za " + fmtLead(lead) + ", u " + time;
    }

    private static String title(String tag, String when) {
        switch (tag) {
            case "zeleno": return "Prijateljski padel " + when;
            case "narancasto": return "HPL liga " + when;
            case "zlatno": return "Rapsa liga " + when;
            case "crveno": return "Otkaži termin " + when;
            case "zuto": return "Dao si termin – " + when;
            default: return "Padel " + when;
        }
    }

    private static String text(String tag, String loc, String who) {
        StringBuilder sb = new StringBuilder();
        switch (tag) {
            case "zeleno": sb.append("Prijateljska partija"); break;
            case "narancasto":
            case "zlatno": sb.append("Ligaška utakmica"); break;
            case "crveno": sb.append("Ne zaboravi otkazati rezervaciju"); break;
            case "zuto":
                sb.append(who.isEmpty() ? "Provjeri je li netko preuzeo termin" : "Termin preuzima: " + who);
                who = "";
                break;
            default: sb.append("Slobodan termin");
        }
        if (!loc.isEmpty()) sb.append(" · ").append(loc);
        if (!who.isEmpty()) sb.append(" · ").append(who);
        return sb.toString();
    }

    // ---------- building ----------

    private static List<Reminder> build(String json, String sharedJson) {
        List<Reminder> out = new ArrayList<>();
        ZoneId zone = ZoneId.systemDefault();
        JSONArray arr;
        try { arr = new JSONArray(json); } catch (Exception e) { return out; }
        List<Integer> shared;
        try { shared = parseLeads(new JSONArray(sharedJson)); } catch (Exception e) { shared = new ArrayList<>(); }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            try {
                String tag = o.optString("tag", "plavo");
                String id = o.optString("id", String.valueOf(i));
                LocalDate date = LocalDate.parse(o.optString("date"));
                String timeStr = o.optString("time", "");
                boolean hasTime = timeStr.matches("\\d{1,2}:\\d{2}");
                JSONArray own = o.optJSONArray("rem");
                List<Integer> leads = own != null ? parseLeads(own) : shared;
                String text = text(tag, o.optString("loc", "").trim(), o.optString("who", "").trim());

                LocalDateTime start = hasTime
                        ? LocalDateTime.of(date, LocalTime.parse(timeStr.length() == 4 ? "0" + timeStr : timeStr))
                        : null;
                Set<Long> seen = new HashSet<>();
                for (int lead : leads) {
                    long at;
                    String w;
                    if (start != null) {
                        // days separately so a DST change keeps the same clock time
                        at = start.minusDays(lead / 1440).minusMinutes(lead % 1440)
                                .atZone(zone).toInstant().toEpochMilli();
                        w = when(lead, timeStr);
                    } else {
                        int days = lead / 1440;
                        at = LocalDateTime.of(date.minusDays(days), LocalTime.of(9, 0))
                                .atZone(zone).toInstant().toEpochMilli();
                        w = dayWord(days);
                    }
                    if (!seen.add(at)) continue;
                    out.add(new Reminder(at, (id + "#" + lead).hashCode(), title(tag, w), text));
                }
            } catch (Exception ignored) {
                // a broken row never stops the others
            }
        }
        return out;
    }
}
