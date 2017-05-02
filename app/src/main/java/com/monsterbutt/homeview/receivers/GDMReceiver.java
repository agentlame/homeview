package com.monsterbutt.homeview.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.services.GDMService;

import java.util.ArrayList;
import java.util.List;


public class GDMReceiver extends BroadcastReceiver {


    public interface ReceiverFinishedCallback {

        public void receiverDone();
    }

    private boolean mIsRunning = false;
    private boolean mFinished = false;
    private List<ReceiverFinishedCallback> mCallback = new ArrayList<ReceiverFinishedCallback>();


    public void startDiscovery(Context context, ReceiverFinishedCallback callback) {

        synchronized (this) {

            if (!mCallback.contains(callback))
                mCallback.add(callback);

            if (!mIsRunning) {

                mIsRunning = true;
                LocalBroadcastManager.getInstance(context).registerReceiver(this, getFilters());
                context.startService(new Intent(context, GDMService.class));
            }
        }
    }

    public boolean isSearchRunning() {

        boolean ret;
        synchronized (this) {

            ret = mIsRunning;
        }
        return ret;
    }

    public boolean isSearchFinished() {

        boolean ret;
        synchronized (this) {

            ret = mFinished;
        }
        return ret;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(GDMService.MSG_RECEIVED)) {

            String ip = intent.getStringExtra("ipaddress").substring(1).trim();
            String data = intent.getStringExtra("data").trim();
            int namePos = 6 + data.indexOf("Name: ");
            int crPos = data.indexOf("\r", namePos);
            PlexServerManager.getInstance(context, null).addServer(new PlexServer(data.substring(namePos, crPos).trim()
                                                                            , ip, context));
        }
        else if (intent.getAction().equals(GDMService.SOCKET_CLOSED)) {

            Log.i("GDMService", "Finished Searching");
            synchronized (this) {

                mIsRunning = false;
                mFinished = true;

                for (ReceiverFinishedCallback callback : mCallback)
                    callback.receiverDone();
            }
        }
    }

    private IntentFilter getFilters() {

        IntentFilter filters = new IntentFilter();
        filters.addAction(GDMService.MSG_RECEIVED);
        filters.addAction(GDMService.SOCKET_CLOSED);

        return filters;
    }
}
