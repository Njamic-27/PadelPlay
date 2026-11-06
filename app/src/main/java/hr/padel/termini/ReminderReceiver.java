package hr.padel.termini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires when a reminder alarm goes off and shows the notification. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Scheduler.ACTION.equals(intent.getAction())) return;
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        int id = intent.getIntExtra("nid", (int) System.currentTimeMillis());
        Notifier.show(context, id, title != null ? title : "Padel termin", text != null ? text : "");
    }
}
