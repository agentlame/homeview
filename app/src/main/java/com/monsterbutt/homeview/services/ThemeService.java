package com.monsterbutt.homeview.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
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

    private static final float VOLUME = 0.15f;

    private static String lastThemePlayed = "";

    MediaPlayer mMediaPlayer = null;

    public static void startTheme(Context context, String themeURL) {
        if (context != null && !TextUtils.isEmpty(themeURL) &&
         SettingsManager.getInstance().getBoolean("preferences_navigation_thememusic")) {

            synchronized (THEME_ALREADY_RUN) {
                lastThemePlayed = themeURL;
                Intent intent = new Intent(context, ThemeService.class);
                intent.setAction(ThemeService.ACTION_PLAY);
                intent.setData(Uri.parse(themeURL));
                context.startService(intent);
            }
        }
    }

    public static void stopTheme(Context context) {

        Intent intent = new Intent(context, ThemeService.class);
        intent.setAction(ThemeService.ACTION_STOP);
        context.startService(intent);
    }

    public static String getLastThemePlayed() {
        String ret;
        synchronized (THEME_ALREADY_RUN) {
            ret = lastThemePlayed;
        }
        return ret;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_PLAY)) {

                try {

                    Uri uri = intent.getData();
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying())
                            mMediaPlayer.stop();
                        mMediaPlayer.reset();
                    } else
                        mMediaPlayer = new MediaPlayer();
                    assert uri != null;
                    mMediaPlayer.setDataSource(getBaseContext(), uri);
                    mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                     .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                     .setUsage(AudioAttributes.USAGE_MEDIA)
                     .build());
                    mMediaPlayer.setOnPreparedListener(this);
                    mMediaPlayer.setOnErrorListener(this);
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setVolume(VOLUME, VOLUME);
                    mMediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals(ACTION_STOP)) {

                stopMedia();
                stopSelf();
            }
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
