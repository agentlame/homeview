package com.monsterbutt.homeview.plex;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.receivers.GDMReceiver;
import com.monsterbutt.homeview.ui.android.ServerLoginDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PlexServerManager implements GDMReceiver.ReceiverFinishedCallback {

    private static PlexServerManager gInstance = null;

    private Activity mCurrentActivity = null;
    private PlexServer                  mSelectedServer = null;
    private HashMap<String, PlexServer> mServers = new HashMap<>();
    private final Context               mContext;

    private GDMReceiver                 mGDMReceiver = new GDMReceiver();
    private boolean gotReceiverCallback         = true;

    public static PlexServerManager getInstance(Context context, Activity currentActivity) {

        if (null == gInstance)
            gInstance = new PlexServerManager(context, currentActivity);
        gInstance.setCurrentActivity(currentActivity);
        return gInstance;
    }

    private PlexServerManager(Context context, Activity currentActivity) {

        mContext = context;
        mCurrentActivity = currentActivity;
        PlexServer server = new PlexServer(mContext);
        if (server.isValid()) {

            mSelectedServer = server;
            addServer(server);
        }
        else
            startDiscovery();
    }

    private void setCurrentActivity(Activity activity) {

        if (activity != null) {
            synchronized (this) {
                mCurrentActivity = activity;
            }
        }
    }

    public void startDiscovery() {

        synchronized (this) {
            gotReceiverCallback = false;
        }
        mGDMReceiver.startDiscovery(mContext, this);
    }

    public void stopDiscovery() {

        synchronized (this) {
            gotReceiverCallback = true;
        }
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mGDMReceiver);
    }

    public boolean addServer(PlexServer server) {

        String name = server.getServerName();
        if (!mServers.containsKey(name)) {

            Log.i(getClass().getName(), "Discovered server : " + name);
            mServers.put(name, server);
            return true;
        }

        Log.i(getClass().getName(), "Server already found : " + name);
        return false;
    }

    public void receiverDone() {

        if (mSelectedServer == null && mServers.size() == 1)
            new ServerCheckTask(mCurrentActivity).getServerToken(mServers.values().iterator().next());
        else {
            synchronized (this) {
                gotReceiverCallback = true;
            }
        }
    }

    public boolean setSelectedServer(PlexServer server) {

        if (server.verifyInstance(mContext)) {

            mSelectedServer = server;
            server.saveAsLastServer(mContext);
            addServer(server);
            return true;
        }
        return false;
    }

    public boolean isDiscoveryRunning() {

        if (mGDMReceiver.isSearchRunning())
            return true;

        boolean ret;
        synchronized (this) {
            ret = !gotReceiverCallback;
        }
        return ret;
    }

    public PlexServer getSelectedServer() {

        return mSelectedServer;
    }

    public final List<PlexServer> getDiscoveredServers() {

        return new ArrayList<>(mServers.values());
    }

    private class ServerCheckTask extends AsyncTask<PlexServer, Void, Boolean> implements ServerLoginDialog.ServerLoginInterface {

        private final Activity activity;

        ServerCheckTask(Activity activity) {
            this.activity = activity;
        }

        void getServerToken(PlexServer server) {
            ServerLoginDialog.login(activity, this, server);
        }

        @Override
        protected Boolean doInBackground(PlexServer... server) {
            return gInstance.setSelectedServer(server[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {

            synchronized (gInstance) {
                gotReceiverCallback = true;
            }
        }

        @Override
        public void onLoginAttempted(PlexServer server, boolean succeeded) {

            if (succeeded)
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);
            else
                Toast.makeText(activity, activity.getString(R.string.plex_login_failed), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginCanceled() {
            // do nothing
        }
    }
}
