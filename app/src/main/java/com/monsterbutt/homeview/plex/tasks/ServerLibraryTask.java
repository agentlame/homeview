package com.monsterbutt.homeview.plex.tasks;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class ServerLibraryTask extends PlexServerTask {

    private MediaContainer mLibrary = null;
    private MediaContainer mSections = null;
    private MediaContainer mHubs = null;
    private MediaContainer mRandomArts = null;

    public ServerLibraryTask(PlexServerTaskCaller caller, PlexServer server) {
        super(caller, server);
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        PlexServer server = getServer();
        MediaContainer library = server.getLibrary();
        MediaContainer hubs = server.getHubs();
        MediaContainer sections = null;
        MediaContainer arts = null;
        List<MediaContainer> libDirs = new ArrayList<>();
        if (library != null) {

            for (Directory dir : library.getDirectories()) {

                if (!dir.getKey().equals("sections"))
                    continue;
                sections = server.getLibraryDir(dir.getKey());
            }
        }

        if (sections != null && sections.getDirectories() != null && !sections.getDirectories().isEmpty()) {

            int sectionPos = (int) (1000 % Math.random()) % sections.getDirectories().size();
            arts = server.getSectionArts(sections.getDirectories().get(sectionPos).getKey());
        }

        synchronized (this) {
            mLibrary = library;
            mSections= sections;
            mHubs = hubs;
            mRandomArts = arts;
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

    public MediaContainer getRandomArts() {

        MediaContainer ret;
        synchronized (this) {
            ret = mRandomArts;
        }
        return ret;
    }

    public MediaContainer getHubs() {

        MediaContainer ret;
        synchronized (this) {
            ret = mHubs;
        }
        return ret;
    }
}
