package com.monsterbutt.homeview.ui.handler;

import android.content.Intent;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.ServerChoiceActivity;
import com.monsterbutt.homeview.ui.fragment.MainFragment;

import java.util.Timer;
import java.util.TimerTask;

public class ServerStatusHandler implements UILifecycleManager.LifecycleListener {

    public interface ServerStatusListener {

        void setSelectedServer(PlexServer server);
    }

    public static final String key = "serverstatushandler";

    public static final int SERVER_CHOICE_RESULT = 1839;
    private static final int SERVER_CHECK_DELAY = 1000;
    private boolean  mForcedServerSelectOnce = false;

    private Timer mServerCheckTimer = null;

    private final MainFragment mFragment;
    private final PlexServerManager mMgr;
    private final ServerStatusListener mListener;

    public ServerStatusHandler(MainFragment fragment, ServerStatusListener listener) {

        mFragment = fragment;
        mListener = listener;
        mServerCheckTimer = new Timer();
        mMgr = PlexServerManager.getInstance(mFragment.getActivity().getApplicationContext());
    }

    private void checkForPlexServer() {

        synchronized (this) {
            if (mMgr.isDiscoveryRunning())
                mServerCheckTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkForPlexServer();
                    }
                }, SERVER_CHECK_DELAY);
            else {

                if (mServerCheckTimer != null)
                    mServerCheckTimer.cancel();
                mServerCheckTimer = null;
                checkServerStatus();
            }
        }
    }

    private void checkServerStatus() {

        PlexServer selected = mMgr.getSelectedServer();
        if (selected != null && selected.isValid()) {

            if (mListener != null)
                mListener.setSelectedServer(selected);
            new ServerLibraryTask(mFragment, selected).execute();
        }
        else if (!mForcedServerSelectOnce) {

            mForcedServerSelectOnce = true;
            mFragment.startActivityForResult(
                                    new Intent(mFragment.getActivity(), ServerChoiceActivity.class),
                                    SERVER_CHOICE_RESULT);
        }
    }

    @Override
    public void onResume() {

        checkForPlexServer();
    }

    @Override
    public void onPause() {

        synchronized (this) {
            if (mServerCheckTimer != null)
                mServerCheckTimer.cancel();
        }
    }

    @Override
    public void onDestroyed() {

        onPause();
    }
}