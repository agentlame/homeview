package com.monsterbutt.homeview.plex.tasks;

import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public class ToggleWatchedStateTask extends AsyncTask<PlexServer, Void, Void> {

    final String key;
    final String ratingKey;
    final boolean isWatched;
    public ToggleWatchedStateTask(PlexLibraryItem item) {

        key = item.getKey();
        ratingKey = Long.toString(item.getRatingKey());
        isWatched = item.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
        item.toggleWatched();
    }

    @Override
    protected Void doInBackground(PlexServer[] params) {

        PlexServer server = params != null && params.length > 0 && params[0] != null ? params[0] : null;
        if (server != null)
            server.toggleWatchedState(key, ratingKey, isWatched);
        return null;
    }
}

