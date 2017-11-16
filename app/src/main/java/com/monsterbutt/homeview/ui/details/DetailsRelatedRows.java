package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;

public class DetailsRelatedRows implements ILifecycleListener {

  private final List<DetailsRelatedRow> rows = new ArrayList<>();

  DetailsRelatedRows(Context context, UILifecycleManager lifecycleManager, PlexServer server,
                     PlexLibraryItem item, ArrayObjectAdapter adapter, Presenter presenter) {
    if (item.getRelated() != null && !item.getRelated().isEmpty()) {
      for (Hub hub : item.getRelated()) {
        DetailsRelatedRow row = new DetailsRelatedRow(context, server, hub, presenter);
        rows.add(row);
        adapter.add(row);
      }
      lifecycleManager.register(DetailsRelatedRows.class.getCanonicalName(), this);
    }
    refresh();
  }

  public void refresh() {
    for (DetailsRelatedRow row : rows)
      row.refresh();
  }

  @Override
  public void onResume(UILifecycleManager lifecycleMgr) {
    refresh();
  }

  @Override
  public void onPause(UILifecycleManager lifecycleMgr) { }

  @Override
  public void onDestroyed(UILifecycleManager lifecycleMgr) {
    lifecycleMgr.unregister(DetailsRelatedRows.class.getCanonicalName());
    for(DetailsRelatedRow row : rows)
      row.release();
  }

}
