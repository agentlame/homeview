package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.ui.presenters.SceneCard;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.LibraryRow;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailTrackingRow;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;

import java.util.List;

class DetailsTimeLineRow {

  static void getRow(Context context, UILifecycleManager lifecycleManager, PlexServer server,
                     PlexLibraryItem parent, List<PlexLibraryItem> items,
                     ArrayObjectAdapter adapter, Presenter presenter) {
    if (parent == null || items == null || items.isEmpty())
      return;
    if (parent instanceof Show)
      adapter.add(new SeasonsRow(context, lifecycleManager, server, (Show) parent, items, presenter));
    else if (parent instanceof PlexVideoItem)
      adapter.add(new ChaptersRow(context, (PlexVideoItem) parent, items, presenter));
  }

  static class SeasonsRow extends LibraryRow implements IDetailTrackingRow, ILifecycleListener {

    private final Show parent;
    private final boolean skipAll;
    private final PlexServer server;
    private final UILifecycleManager lifecycleManager;

    SeasonsRow(Context context, UILifecycleManager lifecycleManager, PlexServer server, Show parent,
               @NonNull List<PlexLibraryItem> items, Presenter presenter) {
      super(context, server, parent.getHeaderForChildren(context), false, presenter);
      this.parent = parent;
      this.skipAll = SettingsManager.getInstance().getBoolean("preferences_navigation_showallseason");
      this.server = server;
      this.lifecycleManager = lifecycleManager;
      lifecycleManager.register(SeasonsRow.class.getCanonicalName(), this);
      addItems(items);
    }

    @Override
    public String getKey() {
      return SeasonsRow.class.getCanonicalName();
    }

    @Override
    protected void refreshData() {
      server.getVideoMetadata(parent.getKey() + "/" + Show.CHILDREN);
    }

    @Override
    protected boolean shouldAdd(PlexLibraryItem item) {
      return !skipAll || !item.getKey().endsWith(Season.ALL_SEASONS);
    }

    @Override
    public int getCurrentIndex() {
      return parent.getSeasonIndex((int) parent.getViewedOffset());
    }

    @Override
    public void onResume() {
      refresh();
    }

    @Override
    public void onPause() { }

    @Override
    public void onDestroyed() { release(); }

    @Override
    public void release() {
      super.release();
      lifecycleManager.unregister(SeasonsRow.class.getCanonicalName());
    }
  }

  static class ChaptersRow extends ListRow implements IDetailTrackingRow {

    private final PlexVideoItem parent;

    ChaptersRow(Context context, PlexVideoItem parent, @NonNull List<PlexLibraryItem> items,
                Presenter presenter) {
      super(parent.getHeaderForChildren(context).hashCode(),
       new HeaderItem(parent.getHeaderForChildren(context).hashCode(), parent.getHeaderForChildren(context)),
       new ArrayObjectAdapter(presenter));
      this.parent = parent;
      ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
      for (PlexLibraryItem item : items)
        adapter.add(new SceneCard(context, item));
    }

    @Override
    public int getCurrentIndex() {
      return parent.getCurrentChapter(parent.getViewedOffset());
    }
  }

}
