package com.monsterbutt.homeview.player.handler;


import android.app.Activity;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;

import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.notifier.UIFragmentNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.android.ResumeChoiceView;
import com.monsterbutt.homeview.ui.android.SelectView;

public class ResumeChoiceHandler implements VideoChangedNotifier.Observer {

  private final Activity mActivity;
  private final UIFragmentNotifier mUiFragmentNotifier;
  private final PlaybackTransportControlGlue mGlue;
  private final SelectView.SelectViewCaller mCaller;

  ResumeChoiceHandler(Activity activity, PlaybackTransportControlGlue glue,
                      SelectView.SelectViewCaller caller, VideoChangedNotifier videoChangedNotifier,
                      UIFragmentNotifier uiFragmentNotifier) {
    mActivity = activity;
    mUiFragmentNotifier = uiFragmentNotifier;
    mGlue = glue;
    mCaller = caller;
    if (videoChangedNotifier != null)
      videoChangedNotifier.register(this);
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                     StartPositionHandler startPosition) {
    mUiFragmentNotifier.releaseSelectView(ResumeChoiceView.class);
    if (item == null)
      return;

    if (startPosition != null && startPosition.getVideoOffset() > 0 &&
     startPosition.getStartType() == StartPositionHandler.PlaybackStartType.Ask) {
      mUiFragmentNotifier.setView(ResumeChoiceView.getView(mActivity, mGlue,
       startPosition.getVideoOffset(), mCaller));
    }
  }

}
