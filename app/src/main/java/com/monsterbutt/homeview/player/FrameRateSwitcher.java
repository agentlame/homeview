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
                listener.shouldPlay(true);
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

        private static final String lock = "lock";
        private static final String Tag = "RRSwitcherReceiver";

        public  RefreshRateSwitchReceiver(Activity activity, FrameRateSwitcherListener listener) {

            this.context = activity.getApplicationContext();
            this.listener = listener;
            delayValue = SettingsManager.getInstance(activity).getLong("preferences_device_refreshrate_delay");
            context.registerReceiver(this, new IntentFilter(intentVal));
        }

        public void setReady() {

            Log.i(Tag, "setReady");
            synchronized (lock) {
                handler.removeCallbacks(timeOutTask);
                handler.postDelayed(timeOutTask,
                 SettingsManager.getInstance(context).getLong("preferences_device_refreshrate_timeout"));
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(intentVal)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.i(Tag, "Unplugged");
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

            synchronized (lock) {

                handler.removeCallbacks(timeOutTask);
                handler.removeCallbacks(delayTask);
                if (timedOut)
                    Log.i(Tag, "Timed out");
                if (delayValue == 0)
                    listener.shouldPlay(true);
                else
                    handler.postDelayed(delayTask, delayValue);
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

    private static final String Tag = "FrameRateSwitcher";

    private final RefreshRateSwitchReceiver receiver;
    private final boolean changeDisplayRate;
    private final Activity activity;

    public FrameRateSwitcher(Activity activity, FrameRateSwitcherListener listener) {

        this.activity = activity;
        changeDisplayRate = SettingsManager.getInstance(activity).getBoolean("preferences_device_refreshrate");
        receiver = new RefreshRateSwitchReceiver(activity, listener);
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

    private static int getBestFrameRate(Display.Mode[] possible, double desired) {

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
            return findBestForTwoPossibleFrames(possible, FILM, UNKNOWN);

        return ret;
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

    public boolean setDisplayRefreshRate(PlexVideoItem item) {

        if (changeDisplayRate && item.hasSourceStats()) {

            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            final Display.Mode[] modes = display.getSupportedModes();
            float preferredFrameRate = convertFrameRate(item.getMedia().get(0).getVideoFrameRate());
            final int requestMode = getBestFrameRate(modes, preferredFrameRate);
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            final int currentModeId = params.preferredDisplayModeId;
            if (currentModeId >= 0 && currentModeId < modes.length) {
                Display.Mode oldMode = modes[currentModeId];
                Display.Mode newMode;
                if (requestMode != -1) {
                    newMode = modes[requestMode];
                    if (oldMode.getRefreshRate() != newMode.getRefreshRate()) {

                        Log.i(Tag, "Changing frame rate to " + Float.toString(newMode.getRefreshRate()));
                        params.preferredDisplayModeId = newMode.getModeId();
                        activity.getWindow().setAttributes(params);
                        receiver.setReady();
                        return true;
                    }
                }
                //else
                //    newMode = modes[currentModeId];
                Log.i(Tag, "Frame rate left alone");
            }
        }
        else
            Log.i(Tag, "Not attempting to change frame rate");
        return false;
    }
}
