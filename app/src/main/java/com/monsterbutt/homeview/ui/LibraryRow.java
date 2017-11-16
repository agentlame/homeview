package com.monsterbutt.homeview.ui;


import android.content.Context;
import android.os.AsyncTask;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.presenters.PosterCardExpanded;
import com.monsterbutt.homeview.ui.presenters.SceneCardExpanded;
import com.monsterbutt.homeview.ui.interfaces.ICardObjectListCallback;
import com.monsterbutt.homeview.ui.interfaces.IMediaObserver;
import com.monsterbutt.homeview.ui.interfaces.IPlexList;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.IContainer;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public abstract class LibraryRow extends ListRow
 implements IPlexList, IMediaObserver, ICardObjectListCallback {

  protected final Context context;
  protected final PlexServer server;
  protected final ArrayObjectAdapter adapter;
  private final boolean useScene;
  protected final StatusWatcher.StatusWatcherObserver statusWatcher;

  private boolean isRefreshing = false;

  public LibraryRow(Context context, PlexServer server, String title,
                    boolean useScene, Presenter presenter) {
    super(title.hashCode(),
     new HeaderItem(title.hashCode(), title),
     new ArrayObjectAdapter(presenter));
    this.useScene = useScene;
    this.statusWatcher = server.registerUIObserver(this);
    this.context = context;
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
  public void statusChanged(IRegisteredMedia media, C.StatusChanged status) {
    int index = adapter.indexOf(media);
    if (index >= 0) {
      if (status == C.StatusChanged.SetDeleted) {
        adapter.remove(media);
        statusWatcher.release(Collections.singletonList((media)));
      }
      else {
        if (status != C.StatusChanged.Refresh) {
          ((PosterCard) adapter.get(index)).setWatchState(status == C.StatusChanged.SetWatched ?
           PlexLibraryItem.WatchedState.Watched : PlexLibraryItem.WatchedState.Unwatched);
        }
        adapter.notifyItemRangeChanged(index, 1);
      }
    }
  }

  private List<CardObject> buildItems(List<PlexLibraryItem> items) {
    List<CardObject> list = new ArrayList<>();
    for (PlexLibraryItem item : items)
      list.add(useScene ? new SceneCardExpanded(context, item) : new PosterCardExpanded(context, item));
    return list;
  }

  protected boolean shouldAdd(PlexLibraryItem item) { return true; }

  protected void addItems(List<PlexLibraryItem> items) {
    LibraryList.fill(adapter, statusWatcher,buildItems(items), null);
  }

  @Override
  public void shouldAdd(IContainer container, PlexLibraryItem item, CardObjectList objects) {
    if (item != null && shouldAdd(item)) {
      objects.add(item.getKey(), useScene ?
       new SceneCardExpanded(context, item) : new PosterCardExpanded(context, item));
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
