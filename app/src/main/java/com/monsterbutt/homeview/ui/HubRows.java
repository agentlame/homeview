package com.monsterbutt.homeview.ui;


import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.interfaces.IHubRows;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;
import com.monsterbutt.homeview.ui.interfaces.IPlexList;
import com.monsterbutt.homeview.ui.interfaces.IServerObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

public abstract class HubRows implements IServerObserver, ILifecycleListener, IHubRows {

  private final Activity activity;
  protected final PlexServer server;
  private final ArrayObjectAdapter adapter;
  private final SelectionHandler selectionHandler;
  private final UILifecycleManager lifeCycleMgr;
  private final StatusWatcher statusWatcher;

  private Map<String, IPlexList> map = new HashMap<>();

  private Task task = null;
  private boolean isRefreshing = false;

  protected HubRows(Fragment fragment, StatusWatcher statusWatcher, PlexServer server,
                    UILifecycleManager lifeCycleMgr, SelectionHandler selectionHandler, ArrayObjectAdapter adapter) {
    this.activity = fragment.getActivity();
    this.server = server;
    this.adapter = adapter;
    this.statusWatcher = statusWatcher;

    new ThemeHandler(lifeCycleMgr, activity, null, true);
    lifeCycleMgr.register(HubRows.class.getCanonicalName(), this);
    this.selectionHandler = selectionHandler;
    this.lifeCycleMgr = lifeCycleMgr;
  }

  @Override
  public void onResume(UILifecycleManager lifeCycleMgr) {
    refresh();
  }

  @Override
  public void onPause(UILifecycleManager lifeCycleMgr) { }

  @Override
  public void onDestroyed(UILifecycleManager lifeCycleMgr) {
    release(lifeCycleMgr);
  }

  @Override
  public void release() {
    release(lifeCycleMgr);
  }

  @Override
  public synchronized void refresh() {
    if (isRefreshing)
      return ;
    isRefreshing = true;
    task = new Task(this);
    task.execute();
  }

  private synchronized void release(UILifecycleManager lifeCycleMgr) {

    lifeCycleMgr.unregister(HubRows.class.getCanonicalName());
    if (task != null)
      task.cancel(true);
    task = null;
    isRefreshing = false;

    for (int i = getRowStartIndex(); i < adapter.size(); ++i) {
      Object o = adapter.get(i);
      if (o instanceof IServerObserver)
        ((IServerObserver) o).release();
    }
  }

  protected abstract int getRowStartIndex();

  protected abstract boolean shouldBeLandscape(Hub hub);

  @Override
  public void updateHubs(List<Hub> hubs) {
    Map<String, Hub> newKeys = new HashMap<>();
    for (Hub hub : hubs)
      newKeys.put(hub.getHubIdentifier(), hub);

    List<String> toRemove = new ArrayList<>();
    for (IPlexList item : map.values()) {
      String key = item.getKey();
      if (null == newKeys.get(key)) {
        item.release();
        adapter.remove(item);
        toRemove.add(key);
      }
    }
    for(String key : toRemove)
      map.remove(key);

    int index = getRowStartIndex();
    for (Hub hub : hubs) {
      String key = hub.getHubIdentifier();
      IPlexList item = map.get(key);
      if (item == null) {
        boolean isLandscape = shouldBeLandscape(hub);
        String path = hub.getKey();
        String header = hub.getTitle();
        for (String sub : activity.getString(R.string.main_rows_header_strip).split(";"))
          header = header.replace(sub, "").trim();
        item = new HubRow(activity, statusWatcher, server, new HubInfo(header, key, path),
         isLandscape, new CardPresenter(server, selectionHandler, true));
        map.put(key, item);
        adapter.add(index, item);
      }
      else
        item.refresh();
      ++index;
    }
    isRefreshing = false;
  }

  private static class Task extends AsyncTask<Void, Void, List<Hub>> {

    private final IHubRows hubRows;

    Task(IHubRows hubRows) {
      this.hubRows = hubRows;
    }

    @Override
    protected List<Hub> doInBackground(Void... voids) {
      MediaContainer mc = hubRows.getHubs();
      List<Hub> hubs =  mc != null && mc.getHubs() != null ? mc.getHubs() : new ArrayList<Hub>();
      Iterator<Hub> iter = hubs.iterator();
      while(iter.hasNext()) {
        Hub hub = iter.next();
        if ((hub.getDirectories() == null || hub.getDirectories().isEmpty()) &&
         (hub.getVideos() == null || hub.getVideos().isEmpty()))
          iter.remove();
      }
      return hubs;
    }

    @Override
    protected void onPostExecute(List<Hub> hubs) {
      hubRows.updateHubs(hubs);
    }

  }

}
