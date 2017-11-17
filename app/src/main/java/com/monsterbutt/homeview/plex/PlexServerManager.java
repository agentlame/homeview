package com.monsterbutt.homeview.plex;

import android.util.Log;

import com.monsterbutt.Homeview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PlexServerManager {


    private static PlexServerManager gInstance = null;

    private PlexServer                  mSelectedServer = null;
    private HashMap<String, PlexServer> mServers = new HashMap<>();

    public static PlexServerManager getInstance() {

        if (null == gInstance)
            gInstance = new PlexServerManager();
        return gInstance;
    }

    private PlexServerManager() {

        PlexServer server = new PlexServer(Homeview.getAppContext());
        if (server.isValid()) {
            mSelectedServer = server;
            addServer(server);
        }
    }

    public void addServer(PlexServer server) {

        String name = server.getServerName();
        if (!mServers.containsKey(name)) {

            Log.i(getClass().getName(), "Discovered server : " + name);
            mServers.put(name, server);
        }
        else {
            PlexServer existing = mServers.get(name);
            if (!existing.isValid() && server.isValid()) {
                Log.i(getClass().getName(), "Updating Server: " + name);
                mServers.put(name, server);
            }
            else
                Log.i(getClass().getName(), "Server already found : " + name);
        }
    }

    public boolean setSelectedServer(PlexServer server) {

        if (server.verifyInstance()) {
            mSelectedServer = server;
            server.saveAsLastServer(Homeview.getAppContext());
            addServer(server);
            return true;
        }
        return false;
    }

    public PlexServer getSelectedServer() {

        return mSelectedServer;
    }

    public final List<PlexServer> getDiscoveredServers() {

        return new ArrayList<>(mServers.values());
    }

}
