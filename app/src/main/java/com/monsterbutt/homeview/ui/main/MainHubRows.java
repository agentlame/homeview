package com.monsterbutt.homeview.ui.main;

import android.app.Fragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.ui.HubRows;
import com.monsterbutt.homeview.ui.UILifecycleManager;

import java.util.HashMap;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


class MainHubRows extends HubRows {

  private final static int ROW_START_INDEX = 1;

  private final HashMap<String, Integer> landscape;

  MainHubRows(Fragment fragment, StatusWatcher statusWatcher, PlexServer server,
              UILifecycleManager lifeCycleMgr, SelectionHandler selectionHandler,
              ArrayObjectAdapter rowsAdapter) {
    super(fragment, statusWatcher, server, lifeCycleMgr, selectionHandler, rowsAdapter);

    landscape = makeOrderHashFromStringDelim(fragment.getString(R.string.main_landscape_rows));
  }

  @Override
  protected int getRowStartIndex() { return ROW_START_INDEX ; }

  @Override
  protected boolean shouldBeLandscape(Hub hub) {
    return landscape.containsKey(hub.getHubIdentifier());
  }

  @Override
  public MediaContainer getHubs() { return server.getHubs(); }

  private static HashMap<String, Integer> makeOrderHashFromStringDelim(String str) {
    String[] tokens = str.split(";");
    HashMap<String, Integer> map = new HashMap<>(tokens.length);
    Integer index = 0;
    for (String token : tokens)
      map.put(token, index++);
    return map;
  }

}
