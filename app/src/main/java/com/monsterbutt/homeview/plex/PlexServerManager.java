package com.monsterbutt.homeview.plex;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.receivers.GDMReceiver;
import com.monsterbutt.homeview.ui.android.ServerLoginDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PlexServerManager implements GDMReceiver.ReceiverFinishedCallback {

    public static final int PICK_CREDENTIALS = 12;
    public static final int ALLOW_CREDENTIAL_SIGNIN = 13;

    public interface CredentialRequestor {
        FragmentActivity getActivity();
        void setCredentialProcessor(CredentialProcessor processor, final Credential credentials);
    }

    public interface CredentialProcessor {
        void processRetrievedCredential(Credential credential);
        void retrievalFailed();
    }

    private static PlexServerManager gInstance = null;

    private FragmentActivity            mActivity = null;
    private PlexServer                  mSelectedServer = null;
    private HashMap<String, PlexServer> mServers = new HashMap<>();
    private final Context               mContext;

    private GDMReceiver                 mGDMReceiver = new GDMReceiver();
    private boolean gotReceiverCallback         = true;

    public static PlexServerManager getInstance(Context context, FragmentActivity activity) {

        if (null == gInstance)
            gInstance = new PlexServerManager(context, activity);
        gInstance.setCurrentActivity(activity);
        return gInstance;
    }

    private PlexServerManager(Context context, FragmentActivity activity) {

        mContext = context;
        mActivity = activity;
        PlexServer server = new PlexServer(mContext);
        if (server.isValid()) {

            mSelectedServer = server;
            addServer(server);
        }
        else
            startDiscovery();
    }

    private void setCurrentActivity(FragmentActivity activity) {

        if (activity != null) {
            synchronized (this) {
                mActivity = activity;
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

        if (mSelectedServer == null && mServers.size() == 1) {
            if (mActivity instanceof CredentialRequestor)
                new ServerCheckTask((CredentialRequestor) mActivity, false).getServerToken(mServers.values().iterator().next());
        }
        else {
            synchronized (this) {
                gotReceiverCallback = true;
            }
        }
    }

    private boolean setSelectedServer(PlexServer server) {

        if (server.verifyInstance(mContext)) {

            mSelectedServer = server;
            server.saveAsLastServer(mContext);
            addServer(server);
            return true;
        }
        return false;
    }


    public void getServerToken(PlexServer server, CredentialRequestor requestor) {

        new ServerCheckTask(requestor, true).getServerToken(server);
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

    private class ServerCheckTask extends AsyncTask<PlexServer, Void, Boolean>
     implements ServerLoginDialog.ServerLoginInterface {

        private final CredentialRequestor requestor;

        private final boolean isSwitchingFromCurrent;
        private boolean isRun = false;

        ServerCheckTask(@NonNull CredentialRequestor requestor, boolean isSwitchingFromCurrent) {
            this.requestor = requestor;
            this.isSwitchingFromCurrent = isSwitchingFromCurrent;
        }

        void getServerToken(PlexServer server) {
            new ServerLoginDialog(requestor.getActivity(), this).login(server);
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

            if (succeeded) {
                ServerCheckTask task = this;
                if (isSwitchingFromCurrent && isRun)
                    task = new ServerCheckTask(requestor, true);
                isRun = true;
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);
            }
            else
                Toast.makeText(requestor.getActivity(), requestor.getActivity().getString(R.string.plex_login_failed), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginCanceled() {
            if (!isSwitchingFromCurrent)
                requestor.getActivity().finish();
        }

        @Override
        public void setCredentialProcessor(PlexServerManager.CredentialProcessor processor, final Credential credentials) {
            requestor.setCredentialProcessor(processor, credentials);
        }
    }
}
