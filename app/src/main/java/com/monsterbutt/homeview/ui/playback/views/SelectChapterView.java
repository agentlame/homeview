package com.monsterbutt.homeview.ui.playback.views;


import android.app.Activity;

import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.ui.presenters.PosterCard;

public class SelectChapterView extends SelectViewRow {

  private final ChapterSelectionNotifier mChapterSelectionNotifier;

  public SelectChapterView(Activity activity, ChapterSelectionNotifier chapterSelectionNotifier) {
    super(activity);
    mChapterSelectionNotifier = chapterSelectionNotifier;
  }

  protected  int getHeight() { return 800; }

  protected String getTag() { return "chapters"; }

  protected void cardClicked(PosterCard card) {
    mChapterSelectionNotifier.chapterSelected(((Chapter) card.getItem()));
  }
}
