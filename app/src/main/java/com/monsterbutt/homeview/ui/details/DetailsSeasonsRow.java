package com.monsterbutt.homeview.ui.details;


import android.app.Activity;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.LibraryRow;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItem;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateNotifier;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateListener;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRow;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRowNotifier;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

class DetailsSeasonsRow extends LibraryRow implements IDetailsScrollRow, IDetailsItemUpdateListener {

  private IDetailsItem parent;
  private final boolean skipAll;
  private final IDetailsScrollRowNotifier scroller;

  DetailsSeasonsRow(Activity activity, StatusWatcher statusWatcher,
                    IDetailsItemUpdateNotifier notifier, PlexServer server, String title,
                    IDetailsScrollRowNotifier scroller, Presenter presenter) {
    super(activity, statusWatcher, server, title, false, presenter);
    this.skipAll = !SettingsManager.getInstance().getBoolean("preferences_navigation_showallseason");
    this.scroller = scroller;
    notifier.register(this);
  }

  @Override
  public String getKey() {
    return DetailsSeasonsRow.class.getCanonicalName();
  }

  @Override
  protected synchronized void refreshData() {
    new SeasonRowTask(this).execute();
  }

  @Override
  protected boolean shouldAdd(PlexLibraryItem item) {
    return !skipAll || !item.getKey().endsWith(Season.ALL_SEASONS);
  }

  @Override
  public synchronized int getCurrentIndex() {
    if (parent == null)
      return 0;
    Show show = (Show) parent.item();
    return show.getSeasonIndex((int) show.getViewedOffset());
  }

  @Override
  public synchronized void update(IDetailsItem obj) {
    PlexLibraryItem item = obj.item();
    if (item instanceof Show) {
      parent = obj;
      refresh();
      scroller.notifiy(getCurrentIndex());
    }
  }

  private static class SeasonRowTask extends LibraryRow.Task {

    private final DetailsSeasonsRow row;

    SeasonRowTask(DetailsSeasonsRow row) {
      super(row);
      this.row = row;
    }

    @Override
    protected MediaContainer getData() {
      return row.parent.server().getVideoMetadata(row.parent.item().getKey() + "/" + Show.CHILDREN);
    }
  }

}
