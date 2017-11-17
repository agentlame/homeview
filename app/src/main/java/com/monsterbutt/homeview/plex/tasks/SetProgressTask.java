package com.monsterbutt.homeview.plex.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public class SetProgressTask extends AsyncTask<Context, Void, Boolean> {

  private final PlexServer server;
  private final String     key;
  private final String     ratingKey;
  private final PlexLibraryItem.WatchedState status;
  private final long offset;

  SetProgressTask(PlexServer server, String key, String ratingKey, long offset) {
    this(server, key, ratingKey, PlexLibraryItem.WatchedState.Refreshed, offset);
  }

  public SetProgressTask(PlexServer server, String key, String ratingKey, PlexLibraryItem.WatchedState status) {
    this(server, key, ratingKey, status, 0);
  }

  private SetProgressTask(PlexServer server, String key, String ratingKey,
                          PlexLibraryItem.WatchedState status, long offset) {
    this.server = server;
    this.key = key;
    this.ratingKey = ratingKey;
    this.status = status;
    this.offset = offset;
  }

  @Override
  protected Boolean doInBackground(Context[] params) {
    Context context = params != null && params.length > 0 ? params[0] : null;
    boolean ret = false;
    switch (status) {
      case Watched:
        ret = server.setWatched(key, ratingKey, context);
        break;
      case Unwatched:
        ret = server.setUnwatched(key, ratingKey, context);
        break;
      case Refreshed:
        ret = server.setProgress(key, ratingKey, offset);
        break;
    }
    return ret;
  }
}
