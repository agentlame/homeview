package com.monsterbutt.homeview.player;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.settings.SettingsManager;

public class FrameRateSwitcher {

    private static class RefreshRateSwitchReceiver extends BroadcastReceiver {

        Handler handler = new Handler();
        Runnable delayTask = new Runnable() {
            @Override
            public void run() {

                Log.d(Tag, "Delayed play over");
                long time = System.currentTimeMillis();
                if ((delayStartTime + delayValue) > time)
                    Log.e(Tag, "Fired before delay over");
                shouldStartPlay();
            }
        };
        Runnable timeOutTask = new Runnable() {
            @Override
            public void run() {
                notifySwitchOccurred(true);
            }
        };

        private final static String intentVal = "android.media.action.HDMI_AUDIO_PLUG";

        private final Context context;
        private final FrameRateSwitcherListener listener;
        private boolean isPlugged = true;
        private final long delayValue;
        private final long timeOutValue;
        private final FrameRateSwitcher switcher;

        private long delayStartTime = 0;
        private long timeOutStartTime = 0;
        private boolean handled = false;

        private int retryCount = 0;
        private final int maxRetryCount = 3;

        private static final String lock = "lock";
        private static final String Tag = "HV_RRSwitcherReceiver";

        public  RefreshRateSwitchReceiver(Activity activity, FrameRateSwitcher switcher, FrameRateSwitcherListener listener) {

            this.switcher = switcher;
            this.context = activity.getApplicationContext();
            this.listener = listener;
            delayValue = SettingsManager.getInstance(activity).getLong("preferences_device_refreshrate_delay");
            Log.d(Tag, "Registering Receiver");
            context.registerReceiver(this, new IntentFilter(intentVal));
            timeOutValue = SettingsManager.getInstance(context).getLong("preferences_device_refreshrate_timeout");
        }

        public void setReady(Activity activity, WindowManager.LayoutParams params) {

            Log.i(Tag, "setReady");
            synchronized (lock) {
                handler.removeCallbacks(timeOutTask);
                timeOutStartTime = System.currentTimeMillis();
                if(timeOutValue > 0) {
                    handler.postDelayed(timeOutTask, timeOutValue);
                    activity.getWindow().setAttributes(params);
                }
                else {
                    activity.getWindow().setAttributes(params);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(timeOutTask, timeOutValue);
                }
            }
        }

        private void shouldStartPlay() {

            if (switcher.isRightMode() || ++retryCount >= maxRetryCount) {
                Log.i(Tag, "Switcher ready for play");
                listener.shouldPlay(true);
            }
            else {
                Log.i(Tag, "Retrying frame rate switch (" + retryCount + " of " + maxRetryCount + ")");
                switcher.setDisplayRefreshRate(true);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(intentVal)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.i(Tag, "Unplugged, stopping playback");
                        isPlugged = false;
                        listener.shouldPlay(false);
                        break;
                    case 1:
                        if (!isPlugged) {

                            Log.i(Tag, "Plugged");
                            isPlugged = true;
                            notifySwitchOccurred(false);
                        }
                        else
                            Log.i(Tag, "Already Plugged in");
                        break;
                }
            }
        }

        private void notifySwitchOccurred(boolean timedOut) {

            Log.i(Tag, "Notify Switch Occured : " + (timedOut ? "TimedOut" : "Received"));
            synchronized (lock) {

                if (handled) {
                    Log.i(Tag, "Already handled");
                    return;
                }
                handled = true;
                long time = System.currentTimeMillis();
                if (timedOut && (timeOutStartTime + timeOutValue) > time)
                    Log.e(Tag, "Fired before timeout");
                handler.removeCallbacks(timeOutTask);
                handler.removeCallbacks(delayTask);
                if (delayValue == 0)
                    shouldStartPlay();
                else {
                    delayStartTime = time;
                    Log.d(Tag, "Delaying for : " + delayValue);
                    handler.postDelayed(delayTask, delayValue);
                }
            }
        }

        public void unregister() {
            Log.i(Tag, "Unregistering Receiver");
            context.unregisterReceiver(this);
        }
    }

    public interface FrameRateSwitcherListener {

        void shouldPlay(boolean play);
    }

    private static final float UNKNOWN = (float)0.0;
    private static final float FILM = (float)23.976;
    private static final float PAL_FILM = (float)25.0;
    private static final float NTSC_INTERLACED = (float)29.97;
    private static final float DIGITAL_30 = (float)30.0;
    private static final float PAL = (float)50.0;
    private static final float NTSC = (float)59.94;
    private static final float DIGITAL_60 = (float) 60.0;

    private static final String Tag = "HV_FrameRateSwitcher";

    private final RefreshRateSwitchReceiver receiver;
    private final boolean changeDisplayRate;
    private final Activity activity;
    private PlexVideoItem item = null;
    private Display.Mode requestedMode = null;

    public FrameRateSwitcher(Activity activity, FrameRateSwitcherListener listener) {

        this.activity = activity;
        changeDisplayRate = SettingsManager.getInstance(activity).getBoolean("preferences_device_refreshrate");
        receiver = new RefreshRateSwitchReceiver(activity, this, listener);
    }

    public void unregister() {
        receiver.unregister();
    }

    private static final int NO_MATCH = -1;
    private static int hasFrameRate(Display.Mode[] possible, double desired) {

        int match = NO_MATCH;
        double matchDiff = 10000;
        for (int i = 0; i < possible.length; ++i) {

            double curr = possible[i].getRefreshRate();
            if (curr == desired)
                return i;

            if (Math.floor(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }
            else if (Math.ceil(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }

        }
        return match;
    }

    private static int findBestForTwoPossibleFrames(Display.Mode[] possible, double desired, double backup) {

        int matchA = hasFrameRate(possible, desired);
        if (NO_MATCH != matchA && desired == possible[matchA].getRefreshRate())
            return matchA;
        int matchB = hasFrameRate(possible, backup);
        if (UNKNOWN != backup && NO_MATCH != matchB) {
            if (NO_MATCH != matchA) {

                double discrepencyA = getFrameDiff(desired, possible[matchA].getRefreshRate());
                double discrepencyB = getFrameDiff(desired, possible[matchB].getRefreshRate());
                if (discrepencyA < discrepencyB)
                    return matchA;
                return matchB;
            }
            else
                return matchB;
        }
        else if (NO_MATCH != matchA)
            return matchA;
        return -1;
    }

    private Display.Mode getBestFitMode(double desired) {

        Display.Mode[] possible = getModes();
        if (possible == null || possible.length == 0) {
            Log.e(Tag, "Cannot find best fit because no modes");
            return null;
        }

        int ret = -1;
        if (desired == DIGITAL_60)
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_60, NTSC);
        else if (desired == NTSC)
            ret = findBestForTwoPossibleFrames(possible, NTSC, DIGITAL_60);
        else if (desired == PAL)
            ret = findBestForTwoPossibleFrames(possible, PAL, UNKNOWN);
        else if (desired == DIGITAL_30) {
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
            if (ret == -1)
                ret =findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
        }
        else if (desired == NTSC_INTERLACED) {
            ret = findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
            if (ret == -1)
                ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
        }
        else if (desired == PAL_FILM)
            ret = findBestForTwoPossibleFrames(possible, PAL_FILM, PAL);
        else if (desired == FILM)
            ret = findBestForTwoPossibleFrames(possible, FILM, UNKNOWN);

        return ret != -1 ? possible[ret] : null;
    }

    private static float convertFrameRate(String frameRate) {

        float ret = UNKNOWN;
        if (TextUtils.isEmpty(frameRate))
            return UNKNOWN;
        if (frameRate.equals("PAL") || frameRate.startsWith("50"))
            ret = PAL;
        else if (frameRate.equals("24p") || frameRate.startsWith("23"))
            ret = FILM;
        else if (frameRate.equals("NTSC") || frameRate.startsWith("59"))
            ret = NTSC;
        else if (frameRate.startsWith("25"))
            ret = PAL_FILM;
        else if (frameRate.startsWith("29"))
            ret = NTSC_INTERLACED;
        else if (frameRate.startsWith("30"))
            ret = DIGITAL_30;
        else if (frameRate.startsWith("60"))
            ret = DIGITAL_60;
        return ret;
    }

    private static double getFrameDiff(double a, double b) {
        return  Math.abs(a - b);
    }


    public boolean setDisplayRefreshRate(PlexVideoItem item, boolean force) {
        this.item = item;
        return setDisplayRefreshRate(force);
    }

    private Display.Mode[] getModes() {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return display.getSupportedModes();
    }

    private Display.Mode getCurrentMode() {
        Display.Mode[] modes = getModes();
        if (modes.length == 0) {
            Log.e(Tag, "Cannot find current modes because of an empty list");
            return null;
        }
        int currentModeId = activity.getWindow().getAttributes().preferredDisplayModeId;
        if (currentModeId == 0)
            currentModeId = 1;
        for (Display.Mode mode : modes) {
            if (mode.getModeId() == currentModeId)
                return mode;
        }
        Log.e(Tag, "Bad preferedDisplayModeId");
        return null;
    }

    private boolean isRightMode() {
        Display.Mode currentMode = getCurrentMode();
        return (requestedMode != null && currentMode != null && currentMode.getModeId() == requestedMode.getModeId());
    }

    private boolean setDisplayRefreshRate(boolean force) {
        if (changeDisplayRate && item.hasSourceStats()) {
            Display.Mode currentMode = getCurrentMode();
            Display.Mode newMode = getBestFitMode(convertFrameRate(item.getMedia().get(0).getVideoFrameRate()));
            if (currentMode != null) {
                if (force || newMode != null && currentMode.getRefreshRate() != newMode.getRefreshRate())
                    return changeMode(newMode);
                Log.i(Tag, "Frame rate left alone");
            }
            else {
                Log.e(Tag, "Forcing change, Current Mode is bad");
                return changeMode(newMode);
            }
        }
        else
            Log.i(Tag, "Not attempting to change frame rate");
        return false;
    }

    private boolean changeMode(Display.Mode newMode) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (newMode == null || params == null) {
            Log.e(Tag, "Invalid mode change");
            return false;
        }
        requestedMode = newMode;
        Log.i(Tag, "Changing frame rate to :" + Float.toString(requestedMode.getRefreshRate())
         + " Mode ID : " + requestedMode.getModeId());
        params.preferredDisplayModeId = requestedMode.getModeId();
        receiver.setReady(activity, params);
        return true;
    }
}
