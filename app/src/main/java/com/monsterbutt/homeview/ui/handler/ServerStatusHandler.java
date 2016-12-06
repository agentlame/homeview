package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.ServerChoiceActivity;

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

    private final Fragment mFragment;
    private final Activity mActivity;
    private final PlexServerManager mMgr;
    private final ServerStatusListener mListener;
    private final PlexServerTaskCaller mCaller;
    private boolean mSkipResume;

    public ServerStatusHandler(Activity activity, PlexServerTaskCaller caller,
                               ServerStatusListener listener) {
        this(activity, caller, listener, false);
    }

    public ServerStatusHandler(Activity activity, PlexServerTaskCaller caller,
                               ServerStatusListener listener, boolean skipFirstResume) {

        mFragment = null;
        mActivity = activity;
        mListener = listener;
        mCaller = caller;
        mSkipResume = skipFirstResume;
        mServerCheckTimer = new Timer();
        mMgr = PlexServerManager.getInstance(mActivity.getApplicationContext());
    }

    public ServerStatusHandler(Fragment fragment, PlexServerTaskCaller caller,
                               ServerStatusListener listener, boolean skipFirstResume) {

        mFragment = fragment;
        mActivity = fragment.getActivity();
        mListener = listener;
        mCaller = caller;
        mSkipResume = skipFirstResume;
        mServerCheckTimer = new Timer();
        mMgr = PlexServerManager.getInstance(mActivity.getApplicationContext());
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
            new ServerLibraryTask(mCaller, selected).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else if (!mForcedServerSelectOnce) {

            mForcedServerSelectOnce = true;
            Intent intent = new Intent(mActivity, ServerChoiceActivity.class);
            if (mFragment != null)
                mFragment.startActivityForResult(intent, SERVER_CHOICE_RESULT);
            else
                mActivity.startActivityForResult(intent, SERVER_CHOICE_RESULT);
        }
    }

    @Override
    public void onResume() {

        if (!mSkipResume)
            checkForPlexServer();
        mSkipResume = false;
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