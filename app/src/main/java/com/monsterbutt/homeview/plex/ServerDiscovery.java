package com.monsterbutt.homeview.plex;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.monsterbutt.homeview.services.GDMService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ServerDiscovery {

  public interface DiscoveryCallback {
    void serverDiscovered(PlexServer server);
    void discoveryDone(List<PlexServer> servers);
  }

  private Set<PlexServer> servers = new HashSet<>();
  private GDMReceiver gdmReceiver = null;
  private DiscoveryCallback callback = null;

  public synchronized void start(Context context, DiscoveryCallback callback, boolean stopIfPreviousLoaded) {

    this.callback = callback;
    if (stopIfPreviousLoaded) {
      PlexServer server = new PlexServer(context);
      if (server.isValid()) {
        servers.add(server);
        receiverDone();
        return;
      }
    }

    if (gdmReceiver == null || !gdmReceiver.isSearchRunning()) {
      servers.clear();
      gdmReceiver = new GDMReceiver();
      gdmReceiver.startDiscovery(context);
    }
  }

  public synchronized boolean isRunning() {
    return gdmReceiver != null && gdmReceiver.isSearchRunning();
  }

  public synchronized void stop(Context context) {
    if (gdmReceiver != null) {
      gdmReceiver.unregister(context);
      gdmReceiver = null;
    }
  }

  private synchronized void addServer(PlexServer server) {
    servers.add(server);
    if (callback != null)
      callback.serverDiscovered(server);
  }

  private void receiverDone() {
    if (callback != null)
      callback.discoveryDone(new ArrayList<>(servers));
  }

  private class GDMReceiver extends BroadcastReceiver {

    private boolean mIsRunning = false;

    void startDiscovery(Context context) {

      synchronized (this) {

        if (!mIsRunning) {

          mIsRunning = true;
          IntentFilter filters = new IntentFilter();
          filters.addAction(GDMService.MSG_RECEIVED);
          filters.addAction(GDMService.SOCKET_CLOSED);

          Context appContext = context.getApplicationContext();
          LocalBroadcastManager.getInstance(appContext).registerReceiver(this, filters);
          appContext.startService(new Intent(appContext, GDMService.class));
        }
      }
    }

    synchronized void unregister(Context context) {
      if(!mIsRunning)
        return;
      Context appContext = context.getApplicationContext();
      LocalBroadcastManager.getInstance(appContext).unregisterReceiver(this);
      mIsRunning = false;
    }

    synchronized boolean isSearchRunning() {
      return mIsRunning;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

      if (intent == null || intent.getAction() == null)
        return;

      if (intent.getAction().equals(GDMService.MSG_RECEIVED)) {

        String ip = intent.getStringExtra("ipaddress").substring(1).trim();
        String data = intent.getStringExtra("data").trim();
        int namePos = 6 + data.indexOf("Name: ");
        int crPos = data.indexOf("\r", namePos);
        PlexServer server = new PlexServer(data.substring(namePos, crPos).trim()
         , ip, context);
        addServer(server);
      }
      else if (intent.getAction().equals(GDMService.SOCKET_CLOSED)) {

        Log.i("GDMService", "Finished Searching");
        unregister(context);
        receiverDone();
      }
    }

  }

}
