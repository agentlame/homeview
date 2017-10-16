package com.monsterbutt.homeview.player.handler;


import android.support.v17.leanback.app.VideoFragment;

import com.monsterbutt.homeview.player.notifier.SwitchTrackNotifier;
import com.monsterbutt.homeview.player.notifier.UIFragmentNotifier;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.android.SelectView;
import com.monsterbutt.homeview.ui.android.SwitchTrackView;

public class SwitchTrackHandler implements SwitchTrackNotifier.Observer {

  private final VideoFragment mFragment;
  private final PlexServer mServer;
  private final SelectView.SelectViewCaller mCaller;
  private final UIFragmentNotifier mUiFragmentNotifier;
  private final TrackSelector mTrackSelector;

  SwitchTrackHandler(VideoFragment fragment, PlexServer server, SelectView.SelectViewCaller caller,
                            UIFragmentNotifier uiFragmentNotifier,
                            SwitchTrackNotifier switchTrackNotifier,
                            TrackSelector trackSelector) {
    mFragment = fragment;
    mServer = server;
    mCaller = caller;
    mUiFragmentNotifier = uiFragmentNotifier;
    switchTrackNotifier.register(this);
    mTrackSelector = trackSelector;
  }

  @Override
  public void switchTrackSelect(int streamType, MediaTrackSelector tracks) {

    mUiFragmentNotifier.setView(
     SwitchTrackView.getTracksView(mFragment.getActivity(), streamType, tracks, mTrackSelector,
      mServer, mCaller));
  }

}
