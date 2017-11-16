package com.monsterbutt.homeview.ui.playback.views;

import android.app.Activity;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.playback.NextUpFragment;

public class NextUpView extends SelectView {

  private NextUpFragment fragment;
  public NextUpView(Activity activity, PlexServer server, PlexVideoItem video, long timeLeft,
                    NextUpFragment.NextUpCallback callback, SelectViewCaller caller) {
    super(activity);
    fragment = new NextUpFragment(activity, server, video, timeLeft, callback);
    setFragment(fragment, caller);
  }

  @Override
  protected String getTag() {
    return "nextup";
  }

  @Override
  protected int getHeight() { return 600; }

  @Override

  public void release() {
    fragment.release();
    super.release();
  }

  @Override
  protected boolean showPlaybackUIOnFragmentSet() { return false; }

}
