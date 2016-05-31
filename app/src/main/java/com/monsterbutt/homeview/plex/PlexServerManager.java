package com.monsterbutt.homeview.plex;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.receivers.GDMReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PlexServerManager implements GDMReceiver.ReceiverFinishedCallback {

    private static PlexServerManager gInstance = null;

    private PlexServer                  mSelectedServer = null;
    private HashMap<String, PlexServer> mServers = new HashMap<>();
    private final Context               mContext;

    private GDMReceiver                 mGDMReceiver = new GDMReceiver();
    boolean gotReceiverCallback         = true;

    public static PlexServerManager getInstance(Context context) {

        if (null == gInstance)
            gInstance = new PlexServerManager(context);
        return gInstance;
    }

    private PlexServerManager(Context context) {

        mContext = context;
        PlexServer server = new PlexServer(mContext);
        if (server.isValid()) {

            mSelectedServer = server;
            addServer(server);
        }
        else
            startDiscovery();
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
            new ServerCheckTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mServers.values().iterator().next());
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

    private class ServerCheckTask extends AsyncTask<PlexServer, Void, Boolean> {

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
    }
}
