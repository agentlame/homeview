package com.monsterbutt.homeview;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.monsterbutt.homeview.services.UpdateRecommendationsService;


public class BootupActivity extends BroadcastReceiver {
    private static final String TAG = "HV_BootupActivity";

    private static final long INITIAL_DELAY = 5000;
    private static boolean isActive = false;
    private static final String lock = "lock";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null)
            return;

        if (intent.getAction().endsWith(Intent.ACTION_BOOT_COMPLETED))
            BootupActivity.scheduleRecommendationUpdate(context);
    }

    static public void scheduleRecommendationUpdate(Context context) {
        Log.d(TAG, "Scheduling recommendations update");

        context = context.getApplicationContext();
        synchronized (lock) {
            if (!isActive) {
                isActive = true;
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    Intent recommendationIntent = new Intent(context, UpdateRecommendationsService.class);
                    PendingIntent alarmIntent = PendingIntent.getService(context, 0, recommendationIntent, 0);

                    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                     INITIAL_DELAY,
                     AlarmManager.INTERVAL_HALF_HOUR,
                     alarmIntent);
                }
            }
        }
    }
}
