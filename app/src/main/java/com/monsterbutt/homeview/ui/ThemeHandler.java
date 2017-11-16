package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.monsterbutt.Homeview;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;

import java.util.ArrayList;
import java.util.HashSet;

public class ThemeHandler implements ILifecycleListener {

    private final Context context;
    private final boolean mStopThemeOnResume;
    private HashSet<String> playedThemes = new HashSet<>();
    private String currentTheme = "";
    private boolean mContinueTheme = false;

    public ThemeHandler(@NonNull UILifecycleManager lifecycleManager, @NonNull Context context,
                        Intent intent, boolean stopThemeOnResume) {

        this.context = context;
        lifecycleManager.register(ThemeHandler.class.getCanonicalName(), this);
        mStopThemeOnResume = stopThemeOnResume;
        if (intent != null) {
          ArrayList<String> list = intent.getStringArrayListExtra(ThemeService.THEME_ALREADY_RUN);
          if (list != null && !list.isEmpty())
            playedThemes = new HashSet<>(list);
        }
    }

    @Override
    public void onResume(UILifecycleManager lifecycleMgr) {
        mContinueTheme = false;
        String lastPlayed = ThemeService.getLastThemePlayed();
        if (mStopThemeOnResume
         || (!TextUtils.isEmpty(currentTheme) && !TextUtils.isEmpty(lastPlayed) && !currentTheme.equalsIgnoreCase(lastPlayed)))
            ThemeService.stopTheme(context);
    }

    @Override
    public void onPause(UILifecycleManager lifecycleMgr) {

        if (mContinueTheme || playedThemes.isEmpty())
            return;
        ThemeService.stopTheme(context);
    }

    @Override
    public void onDestroyed(UILifecycleManager lifecycleMgr) {
        lifecycleMgr.unregister(ThemeHandler.class.getCanonicalName());
    }


    public Bundle getPlaySelectionBundle(Bundle extras, String key) {

        mContinueTheme = true;
        if (!TextUtils.isEmpty(key) && playedThemes.contains(key)) {
            if (extras == null)
                extras = new Bundle();
            extras.putStringArrayList(ThemeService.THEME_ALREADY_RUN, new ArrayList<>(playedThemes));
        }
        return extras;
    }

    public void startTheme(String key) {

        if (key != null && !playedThemes.contains(key) && !Homeview.isPIPActive()) {
            playedThemes.add(key);
            currentTheme = key;
            ThemeService.startTheme(context, currentTheme);
        }
    }
}
