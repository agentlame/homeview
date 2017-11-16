package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItem;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateNotifier;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateListener;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRow;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRowNotifier;
import com.monsterbutt.homeview.ui.presenters.SceneCard;

class DetailsChaptersRow extends ListRow implements IDetailsScrollRow, IDetailsItemUpdateListener {

  private final Context context;
  private PlexVideoItem parent;
  private final IDetailsScrollRowNotifier scroller;

  DetailsChaptersRow(Context context, IDetailsItemUpdateNotifier notifier, String title,
                     IDetailsScrollRowNotifier scroller, Presenter presenter) {
    super(title.hashCode(), new HeaderItem(title.hashCode(), title), new ArrayObjectAdapter(presenter));
    this.context = context;
    this.scroller = scroller;
    notifier.register(this);
  }
  
  @Override
  public String getKey() {
    return DetailsChaptersRow.class.getCanonicalName();
  }

  @Override
  public synchronized int getCurrentIndex() {
    return parent.getCurrentChapter(parent.getViewedOffset());
  }

  @Override
  public synchronized void update(IDetailsItem obj) {
    PlexLibraryItem item = obj.item();
    if (item instanceof PlexVideoItem) {
      parent = (PlexVideoItem) item;
      ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
      if (adapter.size() == 0) {
        for (PlexLibraryItem chapter : parent.getChildrenItems())
          adapter.add(new SceneCard(context, chapter));
      }
      else
        scroller.notifiy(getCurrentIndex());
    }
  }

}
