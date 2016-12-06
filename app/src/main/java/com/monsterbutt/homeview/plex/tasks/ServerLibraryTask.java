package com.monsterbutt.homeview.plex.tasks;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.HubInfo;

import java.util.ArrayList;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class ServerLibraryTask extends PlexServerTask {

    private MediaContainer mLibrary = null;
    private MediaContainer mSections = null;
    private ArrayList<HubInfo> mHubs = null;

    public ServerLibraryTask(PlexServerTaskCaller caller, PlexServer server) {
        super(caller, server);
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        PlexServer server = getServer();
        MediaContainer library = server.getLibrary();
        MediaContainer hubs = server.getHubs();
        MediaContainer sections = null;
        if (library != null) {

            for (Directory dir : library.getDirectories()) {

                if (!dir.getKey().equals("sections"))
                    continue;
                sections = server.getLibraryDir(dir.getKey());
            }
        }
        synchronized (this) {
            mLibrary = library;
            mSections= sections;
            mHubs = HubInfo.getHubs(hubs);
        }

        return library != null;
    }

    public MediaContainer getLibrary() {

        MediaContainer ret;
        synchronized (this) {
            ret = mLibrary;
        }
        return ret;
    }

    public MediaContainer getSections() {

        MediaContainer ret;
        synchronized (this) {
            ret = mSections;
        }
        return ret;
    }

    public ArrayList<HubInfo> getHubs() {

        ArrayList<HubInfo> ret;
        synchronized (this) {
            ret = mHubs;
        }
        return ret;
    }
}
