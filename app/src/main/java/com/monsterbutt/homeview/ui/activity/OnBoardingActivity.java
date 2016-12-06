package com.monsterbutt.homeview.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.handler.ServerStatusHandler;

public class OnBoardingActivity extends Activity implements PlexServerTaskCaller {

    public static String TitleExtra = "TITLE";
    public static String SectionsExtra = "SECTIONS";
    public static String HubsExtra = "HUBS";

    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        mLifeCycleMgr.put(ServerStatusHandler.key, new ServerStatusHandler(this, this, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        mLifeCycleMgr.resumed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ServerStatusHandler.SERVER_CHOICE_RESULT) {

            PlexServer server = PlexServerManager.getInstance(getApplicationContext()).getSelectedServer();
            if (server == null || !server.isValid())
                setResults(null);
        }
    }

    private void setResults(ServerLibraryTask task) {

        Intent intent = new Intent(this, MainActivity.class);
        if (task != null) {
            intent.putExtra(OnBoardingActivity.TitleExtra, task.getLibrary() != null ? task.getLibrary().getTitle1() : "");
            intent.putExtra(OnBoardingActivity.SectionsExtra, task.getSections());
            intent.putParcelableArrayListExtra(OnBoardingActivity.HubsExtra, task.getHubs());
        }
        startActivity(intent);
    }

    @Override
    public void handlePostTaskUI(Boolean result, PlexServerTask task) {

        if (!(task instanceof ServerLibraryTask))
            return;

        if (isFinishing() || isDestroyed())
            return;
        setResults((ServerLibraryTask) task);
    }
}