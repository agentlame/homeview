package com.monsterbutt.homeview.plex.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

public class VideoProgressTask {

    private final SetProgressTask.VideoId id;
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

        id = new SetProgressTask.VideoId(server, video.getKey(), Long.toString(video.getRatingKey()));
        long durationMs = video.getDurationMs();
        startedLimitEnd = MAX_START_LIMIT;
        finishedLimitStart = calculateFinishedLimit(durationMs);
    }

    public void setProgress(boolean isFinished, long progressMs) {

        if (progressMs > finishedLimitStart) {

            if (previousProgressMs < finishedLimitStart)
                new SetProgressTask(id).execute(SetProgressTask.WATCHED);
        }
        else if (progressMs < startedLimitEnd) {

            if (!isFinished && progressMs < previousProgressMs && previousProgressMs > startedLimitEnd)
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
}
