package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.UILifecycleManager;

public class ThemeHandler implements UILifecycleManager.LifecycleListener {

    public final static String key = "themehandler";

    private final Activity mActivity;
    private final boolean mIsViewForShow;
    private final boolean mStopThemeOnResume;
    private final PlexServer mServer;
    private String themeKey = "";
    private String currentTheme = "";
    private boolean mContinueTheme = false;

    public ThemeHandler(Activity activity, PlexServer server, boolean isViewForShow, boolean stopThemeOnResume) {

        mActivity = activity;
        mServer = server;
        Intent intent = activity.getIntent();
        mIsViewForShow = isViewForShow;
        mStopThemeOnResume = stopThemeOnResume;
        if (intent != null)
            themeKey = intent.getStringExtra(ThemeService.THEME_ALREADY_RUN);
        if (themeKey == null)
            themeKey = "";
    }

    @Override
    public void onResume() {
        mContinueTheme = false;
        String lastPlayed = ThemeService.getLastThemePlayed();
        if (mStopThemeOnResume
         || (!TextUtils.isEmpty(currentTheme) && !TextUtils.isEmpty(lastPlayed) && !currentTheme.equalsIgnoreCase(lastPlayed)))
            ThemeService.stopTheme(mActivity);
    }

    @Override
    public void onPause() {

        if (mContinueTheme || TextUtils.isEmpty(themeKey) ||
                (mActivity != null && mActivity.isFinishing() && mIsViewForShow))
            return;
        ThemeService.stopTheme(mActivity);
    }

    @Override
    public void onDestroyed() {}

    public Bundle getPlaySelectionBundle(Bundle extras, String key) {

        mContinueTheme = true;
        if (key != null && themeKey.contains(key)) {
            if (extras == null)
                extras = new Bundle();
            extras.putString(ThemeService.THEME_ALREADY_RUN, themeKey);
        }
        return extras;
    }

    public void startTheme(String key) {

        if (key != null && !themeKey.contains(key) && !mServer.isPIPActive()) {
            themeKey += key + ";";
            currentTheme = key;
            ThemeService.startTheme(mActivity, currentTheme);
        }
    }
}
