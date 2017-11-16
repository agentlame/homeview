package com.monsterbutt.homeview.ui.sectionhub;

import android.app.Fragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.ui.HubRows;
import com.monsterbutt.homeview.ui.UILifecycleManager;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


class SectionHubRows extends HubRows {

  private final String sectionId;

  SectionHubRows(String sectionId, Fragment fragment, PlexServer server, UILifecycleManager lifeCycleMgr,
                 SelectionHandler selectionHandler, ArrayObjectAdapter adapter) {
    super(fragment, server, lifeCycleMgr, selectionHandler, adapter);
    this.sectionId = sectionId;
  }

  @Override
  public MediaContainer getHubs() { return server.getHubForSection(sectionId); }

  @Override
  protected int getRowStartIndex() { return 0; }

  @Override
  protected boolean shouldBeLandscape(Hub hub) {
    return false;
  }
}
