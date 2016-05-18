package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.UILifecycleManager;


public class ThemeHandler implements UILifecycleManager.LifecycleListener {

    public final static String key = "themehandler";

    private final Activity mActivity;
    private final boolean mIsViewForShow;
    private final boolean mStopThemeOnResume;
    private boolean mThemeAlreadyRun;
    private boolean mContinueTheme = false;

    public ThemeHandler(Activity activity, boolean isViewForShow, boolean stopThemeOnResume) {

        mActivity = activity;
        Intent intent = activity.getIntent();
        mIsViewForShow = isViewForShow;
        mStopThemeOnResume = stopThemeOnResume;
        mThemeAlreadyRun = intent != null && intent.getBooleanExtra(ThemeService.THEME_ALREADY_RUN, false);
    }

    @Override
    public void onResume() {
        mContinueTheme = false;
        if (mStopThemeOnResume)
            ThemeService.stopTheme(mActivity);
    }

    @Override
    public void onPause() {

        if (mContinueTheme || !mThemeAlreadyRun ||
                (mActivity != null && mActivity.isFinishing() && mIsViewForShow))
            return;
        ThemeService.stopTheme(mActivity);
    }

    @Override
    public void onDestroyed() {}

    public Bundle getPlaySelectionBundle(Bundle extras) {

        mContinueTheme = true;
        if (mThemeAlreadyRun) {
            if (extras == null)
                extras = new Bundle();
            extras.putBoolean(ThemeService.THEME_ALREADY_RUN, true);
        }
        return extras;
    }

    public void startTheme(String themeKey) {

        if (!mThemeAlreadyRun)
            mThemeAlreadyRun = ThemeService.startTheme(mActivity, themeKey);
    }
}
