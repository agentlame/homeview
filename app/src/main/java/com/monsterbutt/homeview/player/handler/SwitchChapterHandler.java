package com.monsterbutt.homeview.player.handler;


import android.app.Activity;
import android.support.v17.leanback.widget.ListRow;

import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchChapterNotifier;
import com.monsterbutt.homeview.player.notifier.UIFragmentNotifier;
import com.monsterbutt.homeview.ui.playback.views.SelectChapterView;
import com.monsterbutt.homeview.ui.playback.views.SelectView;

public class SwitchChapterHandler implements SwitchChapterNotifier.Observer {

  private final Activity mActivity;
  private final SelectView.SelectViewCaller mCaller;
  private final UIFragmentNotifier mUIFragmentNotifier;
  private final ChapterSelectionNotifier mChapterSelectionNotifier;

  SwitchChapterHandler(Activity activity, SelectView.SelectViewCaller caller,
                       UIFragmentNotifier uiFragmentNotifier,
                       SwitchChapterNotifier switchChapterNotifier,
                       ChapterSelectionNotifier chapterSelectionNotifier) {
    mActivity = activity;
    mCaller = caller;
    mUIFragmentNotifier = uiFragmentNotifier;
    mChapterSelectionNotifier = chapterSelectionNotifier;
    switchChapterNotifier.register(this);
  }

  @Override
  public void switchChapter(ListRow row, int initialPosition) {
    SelectChapterView view  = new SelectChapterView(mActivity, mChapterSelectionNotifier);
    view.setRow(row, initialPosition, mCaller);
    mUIFragmentNotifier.setView(view);
  }

}
