package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.SceneCard;

import java.util.List;

class DetailsExtrasRow extends ListRow {

  DetailsExtrasRow(Context context, List<PlexLibraryItem> extras, String title, Presenter presenter) {
    super(title.hashCode(), new HeaderItem(title.hashCode(), title),
     new ArrayObjectAdapter(presenter));

    ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
    for (PlexLibraryItem extra : extras)
      adapter.add(new SceneCard(context, extra));
  }
}
