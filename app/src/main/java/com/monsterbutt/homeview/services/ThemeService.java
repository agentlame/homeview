package com.monsterbutt.homeview.services;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.monsterbutt.homeview.settings.SettingsManager;

import java.io.IOException;


public class ThemeService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    public static final String ACTION_PLAY = "com.monsterbutt.homeview.services.ThemeService.action.PLAY";
    public static final String ACTION_STOP = "com.monsterbutt.homeview.services.ThemeService.action.STOP";
    
    public static final String THEME_ALREADY_RUN = "theme_run_already";

    MediaPlayer mMediaPlayer = null;

    public static boolean  startTheme(Activity activity, String themeURL) {

        boolean ret = false;
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed() &&
                SettingsManager.getInstance(activity.getApplicationContext()).getBoolean("preferences_navigation_thememusic")
                && !TextUtils.isEmpty(themeURL)) {

            Intent intent = new Intent(activity, ThemeService.class);
            intent.setAction(ThemeService.ACTION_PLAY);
            intent.setData(Uri.parse(themeURL));
            activity.startService(intent);
            ret = true;
        }
        return ret;
    }

    public static void stopTheme(Activity activity) {

        Intent intent = new Intent(activity, ThemeService.class);
        intent.setAction(ThemeService.ACTION_STOP);
        activity.startService(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(ACTION_PLAY)) {

            try {

                Uri uri = intent.getData();
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying())
                        mMediaPlayer.stop();
                    mMediaPlayer.reset();
                }
                else
                    mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(getBaseContext(), uri);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (intent.getAction().equals(ACTION_STOP)) {

            stopMedia();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopMedia();
    }

    private void stopMedia() {

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        player.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        stopMedia();
        stopSelf();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
    }
}
