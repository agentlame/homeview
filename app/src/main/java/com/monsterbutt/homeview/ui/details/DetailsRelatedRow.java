package com.monsterbutt.homeview.ui.details;

import android.content.Context;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.LibraryList;
import com.monsterbutt.homeview.ui.LibraryRow;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class DetailsRelatedRow extends LibraryRow {

  private final Hub hub;

  DetailsRelatedRow(Context context, PlexServer server, Hub hub, Presenter presenter) {
    super(context, server, hub.getTitle(), false, presenter);
    this.hub = hub;
    LibraryList.update(adapter, statusWatcher, this, hub, null);
  }

  @Override
  public String getKey() {
    return hub.getHubKey();
  }

  @Override
  protected void refreshData() {
    new RelatedRowTask(this).execute();
  }

  private static class RelatedRowTask extends LibraryRow.Task {

    private final DetailsRelatedRow row;

    RelatedRowTask(DetailsRelatedRow row) {
      super(row);
      this.row = row;
    }

    @Override
    protected MediaContainer getData() {
      return row.server.getRelatedForKey(row.hub.getKey());
    }
  }
}
