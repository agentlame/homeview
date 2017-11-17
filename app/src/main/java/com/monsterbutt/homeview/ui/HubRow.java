package com.monsterbutt.homeview.ui;

import android.app.Activity;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.settings.SettingsManager;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class HubRow extends LibraryRow {

  private final HubInfo hub;

  HubRow(Activity activity, StatusWatcher statusWatcher, PlexServer server, HubInfo hub,
         boolean useScene, Presenter presenter) {
    super(activity, statusWatcher, server, hub.name, useScene, presenter);
    this.hub = hub;
    refresh();
  }

  @Override
  public String getKey() {
    return hub.key;
  }

  @Override
  protected void refreshData() { new MainHubTask(this).execute(); }

  private static class MainHubTask extends LibraryRow.Task {

    private final HubRow row;
    private final String maxCount;

    private static final String recentlyAddedKey = "recentlyAdded?type=";
    private static final String limitResultsKey = "&X-Plex-Container-Start=0&X-Plex-Container-Size=";

    MainHubTask(HubRow row) {
      super(row);
      this.row = row;
      this.maxCount = SettingsManager.getInstance().getString("preferences_navigation_hubsizelimit");
    }

    protected MediaContainer getData() {
      HubInfo hub = row.hub;
      if (hub.path.contains(recentlyAddedKey) && Long.valueOf(maxCount) > 0)
        hub = new HubInfo(hub.name, hub.key, hub.path + limitResultsKey + maxCount);
      return row.server.getHubsData(hub);
    }
  }

}
