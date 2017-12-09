package com.monsterbutt.homeview.ui;

import android.app.Activity;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredRow;
import com.monsterbutt.homeview.ui.interfaces.IRowManager;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class HubRow extends LibraryRow implements IRegisteredRow {

  private final HubInfo hub;
  private final IRowManager rowManager;

  HubRow(Activity activity, StatusWatcher statusWatcher, PlexServer server, HubInfo hub,
         boolean useScene, Presenter presenter, IRowManager rowManager) {
    super(activity, statusWatcher, server, hub.name, useScene, presenter);
    this.hub = hub;
    this.rowManager = rowManager;
    refresh();
  }

  @Override
  public String getKey() {
    return hub.key;
  }

  @Override
  protected void refreshData() { new MainHubTask(this).execute(); }

  @Override
  public void update() {
    refresh();
  }

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

    @Override
    protected MediaContainer getData() {
      HubInfo hub = row.hub;
      if (hub.path.contains(recentlyAddedKey) && Long.valueOf(maxCount) > 0)
        hub = new HubInfo(hub.name, hub.key, hub.path + limitResultsKey + maxCount);
      return row.server.getHubsData(hub);
    }

    @Override
    protected void onPostExecute() {
      if (row.adapter.size() == 0 && row.rowManager != null)
        row.rowManager.removeRow(row);
    }

  }

}
