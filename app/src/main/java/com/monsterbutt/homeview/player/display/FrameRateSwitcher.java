package com.monsterbutt.homeview.player.display;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.monsterbutt.homeview.settings.SettingsManager;

public class FrameRateSwitcher {

    private static final String Tag = "HV_FrameRateSwitcher";

    /*
    private static boolean isRightMode(Context context, Display.Mode requestedMode) {
        Display.Mode currentMode = RefreshRateChooser.getCurrentMode(context);
        return (requestedMode != null && currentMode != null && currentMode.getModeId() == requestedMode.getModeId());
    }*/

    public static boolean setDisplayRefreshRate(Activity activity, String refreshRateFormat, boolean force,
                                                PlaybackTransportControlGlue player) {

        if (!TextUtils.isEmpty(refreshRateFormat)) {
            Display.Mode currentMode = RefreshRateChooser.getCurrentMode(activity);
            Display.Mode newMode = RefreshRateChooser.getBestFitMode(activity, refreshRateFormat);
            if (currentMode != null) {
                if (force || newMode != null && currentMode.getRefreshRate() != newMode.getRefreshRate())
                    return changeMode(activity, newMode, player);
                Log.i(Tag, "Frame rate left alone");
            }
            else {
                Log.e(Tag, "Forcing change, Current Mode is bad");
                return changeMode(activity, newMode, player);
            }
        }
        else
            Log.i(Tag, "Not attempting to change frame rate");
        return false;
    }

    private static boolean changeMode(Activity activity, Display.Mode requestedMode,
                                      PlaybackTransportControlGlue player) {
        if (requestedMode == null) {
            Log.e(Tag, "Invalid mode change");
            return false;
        }
        Log.i(Tag, "Changing frame rate to :" + Float.toString(requestedMode.getRefreshRate())
         + " Mode ID : " + requestedMode.getModeId());
        new DefaultDisplayListener(activity, requestedMode, player);
        return true;
    }


    private static class DefaultDisplayListener implements DisplayManager.DisplayListener {

        private final Context context;
        private final PlaybackTransportControlGlue player;

        DefaultDisplayListener(Activity activity, Display.Mode requestedMode,
                               PlaybackTransportControlGlue player) {

            this.context = activity;
            this.player = player;
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
            unregister();
            play();
        }

        private void play() {
            long delayValue = SettingsManager.getInstance(context).getLong("preferences_device_refreshrate_delay");
            if (delayValue == 0) {
                if (player != null)
                    player.play();
            }
            else {
                Log.d(Tag, "Delaying for : " + delayValue);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        handler.removeCallbacks(this);
                        Log.d(Tag, "Delayed play over");
                        if (player != null)
                            player.play();
                    }
                }, delayValue);
            }
        }

        private void unregister() {
            handler.removeCallbacks(timeOutTask);
            DisplayManager displayManager = context.getSystemService(DisplayManager.class);
            if (displayManager != null)
                    displayManager.unregisterDisplayListener(this);
        }

        Handler handler = new Handler();
        Runnable timeOutTask = new Runnable() {
            @Override
            public void run() {
                unregister();
                play();
            }
        };
    }

}
