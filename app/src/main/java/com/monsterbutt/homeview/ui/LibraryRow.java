package com.monsterbutt.homeview.ui;


import android.app.Activity;
import android.os.AsyncTask;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.PosterCardExpanded;
import com.monsterbutt.homeview.ui.presenters.SceneCardExpanded;
import com.monsterbutt.homeview.ui.interfaces.ICardObjectListCallback;
import com.monsterbutt.homeview.ui.interfaces.IMediaObserver;
import com.monsterbutt.homeview.ui.interfaces.IPlexList;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import us.nineworlds.plex.rest.model.impl.IContainer;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public abstract class LibraryRow extends ListRow
 implements IPlexList, IMediaObserver, ICardObjectListCallback {

  protected final Activity activity;
  protected final PlexServer server;
  protected final ArrayObjectAdapter adapter;
  private final boolean useScene;
  protected final StatusWatcher.StatusWatcherObserver statusWatcher;

  private boolean isRefreshing = false;

  public LibraryRow(Activity activity, StatusWatcher statusWatcher, PlexServer server, String title,
                    boolean useScene, Presenter presenter) {
    super(title.hashCode(),
     new HeaderItem(title.hashCode(), title),
     new ArrayObjectAdapter(presenter));
    this.useScene = useScene;
    this.statusWatcher = statusWatcher.registerObserver(this);
    this.activity = activity;
    this.server = server;
    adapter = (ArrayObjectAdapter) getAdapter();
  }

  protected abstract void refreshData();

  @Override
  public synchronized void refresh() {
    if (isRefreshing)
      return;
    isRefreshing = true;
    refreshData();
  }

  @Override
  public synchronized void release() {
    isRefreshing = false;
    Collection<IRegisteredMedia> objects = new ArrayList<>();
    for (int i = 0; i < adapter.size(); ++i)
      objects.add((IRegisteredMedia) adapter.get(i));
    statusWatcher.release(objects);
    adapter.clear();
  }

  @Override
  public void statusChanged(final IRegisteredMedia media, final PlexLibraryItem.WatchedState status) {
    final int index = adapter.indexOf(media);
    if (index >= 0) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (status == PlexLibraryItem.WatchedState.Removed) {
            adapter.remove(media);
            statusWatcher.release(Collections.singletonList((media)));
          }
          else {
            adapter.notifyItemRangeChanged(index, 1);
          }
        }
      });
    }
  }

  protected boolean shouldAdd(PlexLibraryItem item) { return true; }

  @Override
  public void shouldAdd(IContainer container, PlexLibraryItem item, CardObjectList objects) {
    if (item != null && shouldAdd(item)) {
      objects.add(item.getKey(), useScene ?
       new SceneCardExpanded(activity, item) : new PosterCardExpanded(activity, item));
    }
  }

  protected abstract static class Task extends AsyncTask<Void, Void, MediaContainer> {

    private final LibraryRow row;

    public Task(LibraryRow row) {
      this.row = row;
    }

    protected abstract MediaContainer getData();

    @Override
    protected MediaContainer doInBackground(Void... voids) {
      return getData();
    }

    @Override
    protected void onPostExecute(MediaContainer mc) {
      LibraryList.update(row.adapter, row.statusWatcher, row, mc, null);
      row.isRefreshing = false;
    }

  }

}
