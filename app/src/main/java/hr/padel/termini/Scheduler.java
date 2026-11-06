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
import java.util.List;

/**
 * Turns the termini into alarms: one the day before (same time, or 09:00 when the
 * termin has no time) and one an hour before. Every change cancels and rebuilds all.
 */
final class Scheduler {
    static final String ACTION = "hr.padel.termini.REMIND";
    private static final String KEY_COUNT = "alarmCount";

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

        // 2) schedule what is still in the future
        long now = System.currentTimeMillis();
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        int n = 0;
        for (Reminder r : build(Storage.load(c))) {
            if (r.at <= now) continue;
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

    private static String tagLabel(String tag) {
        switch (tag) {
            case "zeleno": return "Prijateljski";
            case "narancasto": return "HPL liga";
            case "zlatno": return "Rapsa liga";
            case "zuto": return "Dao nekom";
            case "crveno": return "Otkazati";
            default: return "Slobodno";
        }
    }

    private static List<Reminder> build(String json) {
        List<Reminder> out = new ArrayList<>();
        ZoneId zone = ZoneId.systemDefault();
        JSONArray arr;
        try { arr = new JSONArray(json); } catch (Exception e) { return out; }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String tag = o.optString("tag", "plavo");
            if ("zuto".equals(tag)) continue; // given away: no reminders
            try {
                String id = o.optString("id", String.valueOf(i));
                LocalDate date = LocalDate.parse(o.optString("date"));
                String timeStr = o.optString("time", "");
                boolean hasTime = timeStr.matches("\\d{1,2}:\\d{2}");

                StringBuilder body = new StringBuilder();
                String loc = o.optString("loc", "").trim();
                String who = o.optString("who", "").trim();
                if (!loc.isEmpty()) body.append(loc);
                if (!who.isEmpty()) { if (body.length() > 0) body.append(" · "); body.append(who); }
                if (body.length() > 0) body.append(" · ");
                body.append(tagLabel(tag));
                String text = body.toString();
                boolean cancel = "crveno".equals(tag);

                if (hasTime) {
                    LocalTime t = LocalTime.parse(timeStr.length() == 4 ? "0" + timeStr : timeStr);
                    LocalDateTime start = LocalDateTime.of(date, t);
                    out.add(new Reminder(
                            start.minusDays(1).atZone(zone).toInstant().toEpochMilli(),
                            (id + "#day").hashCode(),
                            cancel ? "Sutra u " + timeStr + ": otkaži termin" : "Sutra padel u " + timeStr,
                            text));
                    out.add(new Reminder(
                            start.minusHours(1).atZone(zone).toInstant().toEpochMilli(),
                            (id + "#hour").hashCode(),
                            cancel ? "Za sat vremena: otkaži termin u " + timeStr : "Za sat vremena padel u " + timeStr,
                            text));
                } else {
                    out.add(new Reminder(
                            LocalDateTime.of(date.minusDays(1), LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli(),
                            (id + "#day").hashCode(),
                            cancel ? "Sutra: otkaži termin" : "Sutra padel",
                            text));
                }
            } catch (Exception ignored) {
                // a broken row never stops the others
            }
        }
        return out;
    }
}
