package com.monsterbutt.homeview.ui.main.tasks;


import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.BackgroundHandler;

import java.util.concurrent.ThreadLocalRandom;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class GetRandomArtWorkTask extends AsyncTask<Void, Void, String> {

    private final PlexServer server;
    private final BackgroundHandler handler;

    public GetRandomArtWorkTask(BackgroundHandler handler, PlexServer server) {
        this.server = server;
        this.handler = handler;
    }

    @Override
    protected String doInBackground(Void[] params) {

        String artKey = "";
        MediaContainer sections = null;
        MediaContainer library = server.getLibrary();
        if (library != null) {
            for (Directory dir : library.getDirectories()) {
                if (!dir.getKey().equals("sections"))
                    continue;
                sections = server.getLibraryDir(dir.getKey());
            }
        }

        if (sections != null && sections.getDirectories() != null && !sections.getDirectories().isEmpty()) {
            int sectionPos = Math.abs(ThreadLocalRandom.current().nextInt()) % sections.getDirectories().size();
            MediaContainer arts = server.getSectionArts(sections.getDirectories().get(sectionPos).getKey());
            if (arts != null && arts.getPhotos() != null && !arts.getPhotos().isEmpty())
                artKey = arts.getPhotos().get((int) (1000 * Math.random()) % arts.getPhotos().size()).getKey();
        }
        return artKey;
    }

    @Override
    protected void onPostExecute(String artKey) {

        if (artKey != null && !artKey.isEmpty())
            handler.updateBackground(artKey);
    }
}
