package com.monsterbutt.homeview.player.display;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.monsterbutt.homeview.player.notifier.FrameRateSwitchNotifier;
import com.monsterbutt.homeview.settings.SettingsManager;

public class FrameRateSwitcher {

    private static final String Tag = "HV_FrameRateSwitcher";

    /*
    private static boolean isRightMode(Context context, Display.Mode requestedMode) {
        Display.Mode currentMode = RefreshRateChooser.getCurrentMode(context);
        return (requestedMode != null && currentMode != null && currentMode.getModeId() == requestedMode.getModeId());
    }*/

    public static boolean setDisplayRefreshRate(Activity activity, String refreshRateFormat, boolean force,
                                                FrameRateSwitchNotifier notifier) {

        if (!TextUtils.isEmpty(refreshRateFormat)) {
            Display.Mode currentMode = RefreshRateChooser.getCurrentMode(activity);
            Display.Mode newMode = RefreshRateChooser.getBestFitMode(activity, refreshRateFormat);
            if (currentMode != null) {
                if (force || newMode != null && currentMode.getRefreshRate() != newMode.getRefreshRate())
                    return changeMode(activity, newMode, notifier);
                Log.i(Tag, "Frame rate left alone");
            }
            else {
                Log.e(Tag, "Forcing change, Current Mode is bad");
                return changeMode(activity, newMode, notifier);
            }
        }
        else
            Log.i(Tag, "Not attempting to change frame rate");
        return false;
    }

    private static boolean changeMode(Activity activity, Display.Mode requestedMode,
                                      FrameRateSwitchNotifier notifier) {
        if (requestedMode == null) {
            Log.e(Tag, "Invalid mode change");
            return false;
        }
        Log.i(Tag, "Changing frame rate to :" + Float.toString(requestedMode.getRefreshRate())
         + " Mode ID : " + requestedMode.getModeId());
        new DefaultDisplayListener(activity, requestedMode, notifier);
        return true;
    }


    private static class DefaultDisplayListener implements DisplayManager.DisplayListener {

        private final Context context;
        private final FrameRateSwitchNotifier notifier;

        private final Handler handler = new Handler();
        private final Runnable timeOutTask = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };

        DefaultDisplayListener(Activity activity, Display.Mode requestedMode,
                               FrameRateSwitchNotifier notifier) {

            this.context = activity;
            this.notifier = notifier;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                DisplayManager displayManager = context.getSystemService(DisplayManager.class);
                if (displayManager != null) {
                    displayManager.registerDisplayListener(this, null);
                }
            }

            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.preferredDisplayModeId = requestedMode.getModeId();
            activity.getWindow().setAttributes(params);

            long timeOutValue = SettingsManager.getInstance(context).getLong("preferences_device_refreshrate_timeout");
            if (timeOutValue > 0)
                timeOutValue = 10000; // 5 second timeout
            handler.postDelayed(timeOutTask, timeOutValue);
        }

        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {

            int defaultDisplayId = RefreshRateChooser.getDefaultDisplayId(context);
            if (displayId == defaultDisplayId || defaultDisplayId == RefreshRateChooser.DISPLAY_ID_UNKNOWN)
                finish();
        }

        private void finish() {
            unregister();
            if (notifier != null)
                notifier.frameRateSwitched(SettingsManager.getInstance(context).getLong("preferences_device_refreshrate_delay"));
        }

        private void unregister() {
            handler.removeCallbacks(timeOutTask);
            DisplayManager displayManager = context.getSystemService(DisplayManager.class);
            if (displayManager != null)
                    displayManager.unregisterDisplayListener(this);
        }

    }

}
