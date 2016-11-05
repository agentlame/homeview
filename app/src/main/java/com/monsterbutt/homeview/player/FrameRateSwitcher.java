package com.monsterbutt.homeview.player;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.settings.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

public class FrameRateSwitcher {

    private static class RefreshRateSwitchReceiver extends BroadcastReceiver {

        private class TimeOutTask extends TimerTask {

            @Override
            public void run() {

                notifySwitchOccurred(true);
            }
        }

        private final static String intentVal = "android.media.action.HDMI_AUDIO_PLUG";

        public static void RegisterReceiver(Activity activity, HomeViewExoPlayer player,
                                            FrameRateSwitcherListener listener, FrameRateSwitcher switcher) {

            activity.registerReceiver(new RefreshRateSwitchReceiver(activity, player, listener, switcher),
                                      new IntentFilter(intentVal));
        }

        private final Activity activity;
        private final HomeViewExoPlayer player;
        private final FrameRateSwitcherListener listener;
        private final FrameRateSwitcher switcher;
        private boolean isPlugged = true;
        static private boolean isFinished;
        Timer timeoutTimer;
        static private final String lock = "lock";

        private RefreshRateSwitchReceiver(Activity activity, HomeViewExoPlayer player,
                                          FrameRateSwitcherListener listener, FrameRateSwitcher switcher) {

            RefreshRateSwitchReceiver.isFinished = false;
            this.activity = activity;
            this.player = player;
            this.listener = listener;
            this.switcher = switcher;
            timeoutTimer = new Timer();
            timeoutTimer.schedule(new TimeOutTask(),
                    SettingsManager.getInstance(activity).getLong("preferences_device_refreshrate_timeout"));
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(intentVal)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        isPlugged = false;
                        break;
                    case 1:
                        if (!isPlugged) {
                            isPlugged = true;
                            notifySwitchOccurred(false);
                        }
                        break;
                }
            }
        }

        private void notifySwitchOccurred(boolean timedOut) {

            synchronized (lock) {

                if (!RefreshRateSwitchReceiver.isFinished) {

                    RefreshRateSwitchReceiver.isFinished = true;
                    timeoutTimer.cancel();
                    timeoutTimer = null;


                    final WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                    if (timedOut && params.preferredDisplayModeId != switcher.newMode.getModeId()) {

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (params.preferredDisplayModeId != switcher.oldMode.getModeId()) {

                                    params.preferredDisplayModeId = switcher.oldMode.getModeId();
                                    activity.getWindow().setAttributes(params);
                                }
                                Toast.makeText(activity, activity.getString(R.string.refresh_switch_fail), Toast.LENGTH_LONG).show();
                                listener.switchOccured(player, switcher);
                            }
                        });

                    }
                    else {
                        switcher.didSucceed = params.preferredDisplayModeId == switcher.newMode.getModeId();
                        listener.switchOccured(player, switcher);
                    }
                    activity.unregisterReceiver(this);
                }
            }
        }
    }

    public interface FrameRateSwitcherListener {

        void switchOccured(HomeViewExoPlayer player, FrameRateSwitcher switcher);
    }

    private static final float UNKNOWN = (float)0.0;
    private static final float FILM = (float)23.976;
    private static final float PAL_FILM = (float)25.0;
    private static final float NTSC_INTERLACED = (float)29.97;
    private static final float DIGITAL_30 = (float)30.0;
    private static final float PAL = (float)50.0;
    private static final float NTSC = (float)59.94;
    private static final float DIGITAL_60 = (float) 60.0;

    public final Display.Mode oldMode;
    public final Display.Mode newMode;
    public boolean didSucceed = false;

    public final float preferredFrameRate;

    private FrameRateSwitcher(final Display.Mode oldMode, final Display.Mode newMode, float preferredFrameRate) {

        this.oldMode = oldMode;
        this.newMode = newMode;
        this.preferredFrameRate = preferredFrameRate;
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

    public static void setDisplayRefreshRate(Activity activity, final HomeViewExoPlayer player,
                                              final FrameRateSwitcherListener listener) {

        SettingsManager mgr = SettingsManager.getInstance(activity);
        if (mgr.getBoolean("preferences_device_refreshrate") && player.getItem().hasSourceStats()) {

            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            final Display.Mode[] modes = display.getSupportedModes();
            final float preferredFrameRate = convertFrameRate(player.getItem().getMedia().get(0).getVideoFrameRate());
            final int requestMode = getBestFrameRate(modes, preferredFrameRate);
            if (requestMode != -1 && display.getMode().getRefreshRate() != modes[requestMode].getRefreshRate()) {

                WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                final int currentModeId = params.preferredDisplayModeId;
                params.preferredDisplayModeId = modes[requestMode].getModeId();
                RefreshRateSwitchReceiver.RegisterReceiver(activity, player, listener,
                                                           new FrameRateSwitcher(modes[currentModeId], modes[requestMode], preferredFrameRate));
                activity.getWindow().setAttributes(params);
                return;
            }
        }

        if (listener != null)
            listener.switchOccured(player, null);
    }
}
