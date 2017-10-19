package com.monsterbutt.homeview.ui.activity;


import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v4.app.FragmentActivity;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.handler.UIFragmentHandler;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

public class PlaybackActivity extends FragmentActivity implements UIFragmentHandler.UIHandlerRegister {


  public static final String Tag = "HV_PlaybackActivity";
  public static final String ACTION_VIEW = "com.monsterbutt.homeview.ui.activity.action.VIEW";
  public static final String KEY = "key";
  public static final String ACTION = "action";
  public static final String START_OFFSET = "startoffset";
  public static final String VIDEO = "video";
  public static final String TRACKS = "tracks";
  public static final String FILTER = "filter";
  public static final String SHARED_ELEMENT_NAME = "hero";
  public static final String URI = "homeview://app/playback";

  private UIFragmentHandler mUIFragmentHandler;
  private PlaybackTransportControlGlue mPlaybackGlue;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_playback);

    FragmentTransaction ft = getFragmentManager().beginTransaction();
    ft.add(R.id.videoFragment, new PlaybackFragment(), PlaybackFragment.TAG);
    ft.commit();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    // This part is necessary to ensure that getIntent returns the latest intent when
    // VideoExampleActivity is started. By default, getIntent() returns the initial intent
    // that was set from another activity that started VideoExampleActivity. However, we need
    // to update this intent when for example, user clicks on another video when the currently
    // playing video is in PIP mode, and a new video needs to be started.
    setIntent(intent);
  }

  @Override
  public void register(PlaybackTransportControlGlue glue, UIFragmentHandler handler) {
    mPlaybackGlue = glue;
    mUIFragmentHandler = handler;
  }

  @Override
  public void onBackPressed() {
    if (!mUIFragmentHandler.shouldCancelOnBack())
      super.onBackPressed();
  }


  @Override
  public void onVisibleBehindCanceled() {
    if (mPlaybackGlue != null)
      mPlaybackGlue.pause();
    super.onVisibleBehindCanceled();
  }
}
