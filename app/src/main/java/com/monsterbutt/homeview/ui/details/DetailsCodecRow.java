package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRow;
import com.monsterbutt.homeview.ui.presenters.CodecCard;

public class DetailsCodecRow extends ListRow implements IDetailsScrollRow {

  private final int initialIndex;

  DetailsCodecRow(Context context, String title, MediaTrackSelector tracks, int streamType,
                          Presenter presenter) {
    super(title.hashCode(), new HeaderItem(title.hashCode(), title), new ArrayObjectAdapter(presenter));
    ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
    int selectedIndex = 0;
    MediaTrackSelector.StreamChoiceArrayAdapter choices = tracks.getTracks(context, streamType);
    for (int i = 0; i < choices.getCount(); ++i) {
      Stream.StreamChoice choice = choices.getItem(i);
      if (choice == null)
        continue;
      if (choice.isCurrentSelection())
        selectedIndex = i;
      adapter.add(new CodecCard(context, choice, streamType));
    }
    initialIndex = selectedIndex;
  }

  @Override
  public int getCurrentIndex() {
    return initialIndex;
  }

}
