package com.monsterbutt.homeview.plex.tasks;

import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;

public class SetProgressTask extends AsyncTask<Long, Void, Boolean> {

    public static class VideoId {

        public final PlexServer server;
        public final String     key;
        public final String     ratingKey;

        public VideoId(PlexServer server, String key, String ratingKey) {

            this.server = server;
            this.key = key;
            this.ratingKey = ratingKey;
        }
    }

    public final static long UNWATCHED = 0;
    public final static long WATCHED = -1;

    private final VideoId id;

    public SetProgressTask(VideoId id) {
        this.id = id;
    }

    @Override
    protected Boolean doInBackground(Long[] params) {

        if (params == null || params.length == 0)
            return false;

        boolean ret;
        long progressMs = params[0];
        if (progressMs == WATCHED)
            ret = id.server.setWatched(id.key, id.ratingKey);
        else if (progressMs == UNWATCHED)
            ret = id.server.setUnwatched(id.key, id.ratingKey);
        else
            ret = id.server.setProgress(id.key, id.ratingKey, progressMs);

        return ret;
    }
}
