package com.monsterbutt.homeview.ui.main;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.ui.presenters.SectionCard;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;
import com.monsterbutt.homeview.ui.interfaces.IServerObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class MainSectionsRow extends ListRow implements IServerObserver, ILifecycleListener {

  private final Context context;
  private final ArrayObjectAdapter adapter;
  private final PlexServer server;
  private final UILifecycleManager lifeCycleMgr;

  private Task task = null;

  private boolean isRefreshing = false;
  private Map<String, PlexContainerItem> map = new HashMap<>();

  MainSectionsRow(@NonNull Context context, @NonNull PlexServer server,
                         @NonNull UILifecycleManager lifecycleManager, Presenter presenter) {
    super(context.getString(R.string.library).hashCode(),
     new HeaderItem(context.getString(R.string.library).hashCode(), context.getString(R.string.library)),
     new ArrayObjectAdapter(presenter));
    this.context = context;
    this.adapter = (ArrayObjectAdapter) getAdapter();
    this.server = server;
    this.lifeCycleMgr = lifecycleManager;
    lifecycleManager.register(MainSectionsRow.class.getCanonicalName(), this);
    refresh();
  }

  @Override
  public void onResume(UILifecycleManager lifecycleMgr) { refresh(); }

  @Override
  public void onPause(UILifecycleManager lifecycleMgr) { }

  @Override
  public void onDestroyed(UILifecycleManager lifecycleMgr) { release(lifecycleMgr); }

  @Override
  public synchronized void refresh() {
    if (isRefreshing)
      return;
    isRefreshing = true;
    task = new Task(this);
    task.execute();
  }

  @Override
  public void release() {
    release(lifeCycleMgr);
  }

  private synchronized void release(UILifecycleManager lifeCycleMgr) {
    lifeCycleMgr.unregister(MainSectionsRow.class.getCanonicalName());
    if (task != null)
      task.cancel(true);
    task = null;
    isRefreshing = false;
  }

  private synchronized void updateSections(List<PlexContainerItem> sections) {

    Map<String, PlexContainerItem> newKeys = new HashMap<>();
    for (PlexContainerItem item : sections)
      newKeys.put(item.getKey(), item);

    List<String> toRemove = new ArrayList<>();
    for (PlexContainerItem item : map.values()) {
      String key = item.getKey();
      if (null == newKeys.get(key)) {
        adapter.remove(item);
        toRemove.add(key);
      }
    }
    for(String key : toRemove)
      map.remove(key);

    int index = 0;
    for (PlexContainerItem item : sections) {
      if (item != null) {
        long ratingKey = item.getRatingKey();
        String key = ratingKey > 0 ? Long.toString(ratingKey) : item.getKey();
        PlexContainerItem ri = map.get(key);
        if (ri == null) {
          map.put(key, item);
          adapter.add(index, new SectionCard(context, item));
        }
        ++index;
      }
    }

    isRefreshing = false;
  }

  private static class Task extends AsyncTask<Void, Void, List<PlexContainerItem>> {

    final MainSectionsRow row;

    Task(MainSectionsRow row) {
      this.row = row;
    }

    @Override
    protected List<PlexContainerItem> doInBackground(Void... voids) {

      List<PlexContainerItem> sections = new ArrayList<>();
      MediaContainer library = row.server.getLibrary();
      if (library != null) {
        for (Directory directory : library.getDirectories()) {

          if (!directory.getKey().equals("sections"))
            continue;
          MediaContainer mc = row.server.getLibraryDir(directory.getKey());
          for (Directory section : mc.getDirectories()) {
            PlexContainerItem item = PlexContainerItem.getItem(section);
            if (item != null)
              sections.add(item);
          }
          break;
        }
      }

      return sections;
    }

    @Override
    protected void onPostExecute(List<PlexContainerItem> sections) {
      row.updateSections(sections);
    }

  }

}
