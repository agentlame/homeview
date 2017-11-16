package com.monsterbutt.homeview.ui.presenters;


import android.content.Context;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public class SectionCard extends SceneCard {

  public SectionCard(Context context, PlexLibraryItem obj) {
    super(context, obj);
  }

  @Override
  public int getHeight() {
    return R.dimen.CARD_SECTION_HEIGHT;
  }

  @Override
  public int getWidth() { return R.dimen.CARD_SECTION_WIDTH; }

}
