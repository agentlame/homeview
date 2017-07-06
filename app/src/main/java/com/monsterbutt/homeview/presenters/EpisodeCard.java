package com.monsterbutt.homeview.presenters;


import android.content.Context;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public class EpisodeCard extends SceneCard {

  public EpisodeCard(Context context, PlexLibraryItem obj) {
    super(context, obj);
  }

  @Override
  public int getHeight() {
    return R.dimen.CARD_EPISODE_HEIGHT;
  }

  @Override
  public int getWidth() { return R.dimen.CARD_EPISODE_WIDTH; }

}
