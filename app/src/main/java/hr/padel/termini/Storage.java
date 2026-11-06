package hr.padel.termini;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Keeps the list of termini as one JSON array string on the phone. */
final class Storage {
    static final String PREFS = "padel";
    private static final String KEY = "termini";
    private static final String KEY_REM = "podsjetnici";
    /** Shared reminder leads in minutes; default is the old behaviour: 1 day and 1 hour before. */
    private static final String DEFAULT_REM = "[1440,60]";

    private Storage() {}

    static synchronized String load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY, null);
        if (json == null) {
            json = readSeed(c);
            p.edit().putString(KEY, json).apply();
        }
        return json;
    }

    static synchronized void save(Context c, String json) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply();
    }

    static synchronized String loadReminders(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_REM, DEFAULT_REM);
    }

    static synchronized void saveReminders(Context c, String json) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_REM, json).apply();
    }

    /** First start: the termini copied from the notepad. */
    private static String readSeed(Context c) {
        try (InputStream in = c.getAssets().open("seed.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "[]";
        }
    }
}
