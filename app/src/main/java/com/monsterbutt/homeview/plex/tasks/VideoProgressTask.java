package com.monsterbutt.homeview.plex.tasks;

import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

public class VideoProgressTask {

    private final VideoId       id;
    private final long          startedLimitEnd;
    private final long          finishedLimitStart;

    private long                previousProgressMs = 0;

    private static final long   Milli = 1000;
    private static final long   MAX_START_LIMIT = 30 * Milli;

    private static final double   WATCHED_THRESHOLD_PERCENT = 0.93;
    private static final long   MIN_WATCHED_LIMIT = 5 * Milli;

    public static final long    PreferedSpanThresholdMs = 10 * Milli;

    static public VideoProgressTask getTask(PlexServer server, PlexVideoItem video) {
        return new VideoProgressTask(server, video);
    }

    private VideoProgressTask(PlexServer server, PlexVideoItem video) {

        id = new VideoId(server, video.getKey(), Long.toString(video.getRatingKey()));
        long durationMs = video.getDurationMs();
        startedLimitEnd = MAX_START_LIMIT;
        finishedLimitStart = calculateFinishedLimit(durationMs);
    }

    public void setProgress(long progressMs) {

        if (progressMs > finishedLimitStart) {

            if (previousProgressMs < finishedLimitStart)
                new SetProgressTask(id).execute(SetProgressTask.WATCHED);
        }
        else if (progressMs < startedLimitEnd) {

            if (progressMs < previousProgressMs && previousProgressMs > startedLimitEnd)
                new SetProgressTask(id).execute(SetProgressTask.UNWATCHED);
        }
        else
            new SetProgressTask(id).execute(progressMs);

        previousProgressMs = progressMs;
    }

    private static long calculateFinishedLimit(long durationMs) {

        long threshold = (long) ((double)durationMs * WATCHED_THRESHOLD_PERCENT);
        if ((durationMs - threshold) < MIN_WATCHED_LIMIT)
            threshold = Math.max(0, durationMs - MIN_WATCHED_LIMIT);
        return threshold;
    }

    private class VideoId {

        public final PlexServer server;
        public final String     key;
        public final String     ratingKey;

        public VideoId(PlexServer server, String key, String ratingKey) {

            this.server = server;
            this.key = key;
            this.ratingKey = ratingKey;
        }
    }

    private static class SetProgressTask extends  AsyncTask<Long, Void, Boolean> {

        public static long UNWATCHED = 0;
        public static long WATCHED = -1;

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
}
