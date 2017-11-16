package com.monsterbutt.homeview.player.handler;


import android.os.Handler;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.util.Log;

import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.notifier.QueueChangeNotifier;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.notifier.UIFragmentNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.playback.views.NextUpView;
import com.monsterbutt.homeview.ui.playback.views.SelectView;
import com.monsterbutt.homeview.ui.playback.NextUpFragment;

import static com.monsterbutt.homeview.plex.media.PlexVideoItem.NEXTUP_DISABLED;

public class NextUpHandler extends PlaybackGlue.PlayerCallback
 implements VideoChangedNotifier.Observer, NextUpFragment.NextUpCallback,
 QueueChangeNotifier.Observer, SeekOccuredNotifier.Observer {

  private static final String Tag = "HV_NextUpHandler";

  private final VideoFragment mFragment;
  private final PlexServer mServer;
  private final PlaybackTransportControlGlue mGlue;
  private final VideoHandler mVideoHandler;
  private final UIFragmentNotifier mUiFragmentNotifier;
  private final Runnable mRunnable;
  private final Handler mHandler = new Handler();
  private boolean mWasShown = false;

  NextUpHandler(VideoFragment fragment, PlexServer server, PlaybackTransportControlGlue glue,
                       VideoHandler videoHandler, UIFragmentHandler uiHandler,
                       VideoChangedNotifier videoChangedNotifier, QueueChangeNotifier queueChangeNotifier,
                       SeekOccuredNotifier seekOccuredNotifier, UIFragmentNotifier uiFragmentNotifier) {
    mFragment = fragment;
    mUiFragmentNotifier = uiFragmentNotifier;
    mServer = server;
    mGlue = glue;
    mRunnable = new NextUpRunnable(uiHandler);
    mVideoHandler = videoHandler;
    mGlue.addPlayerCallback(this);
    if (videoChangedNotifier != null)
      videoChangedNotifier.register(this);
    if (queueChangeNotifier != null)
      queueChangeNotifier.register(this);
    if (seekOccuredNotifier != null)
      seekOccuredNotifier.register(this);
  }

  @Override
  public void onPlayStateChanged(PlaybackGlue glue) {
    shouldSetupNextUp(mVideoHandler.getCurrentVideo(), mVideoHandler.getNextVideo(), false);
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                     StartPositionHandler startPosition) {
    mUiFragmentNotifier.releaseSelectView(NextUpView.class);
    mWasShown = false;
    mHandler.removeCallbacks(mRunnable);
  }

  @Override
  public void clicked(NextUpFragment.NextUpCallback.Clicked button) {

    mUiFragmentNotifier.releaseSelectView(NextUpView.class);
    switch(button) {

      case StartNext:
        mVideoHandler.playNext();
        break;

      case StopList:
        mVideoHandler.cancelNext();
        break;

      case ShowList:
        mVideoHandler.showQueue();
        break;

      default:
        break;
    }
  }

  @Override
  public void queueChanged(PlexVideoItem previous, PlexVideoItem current, PlexVideoItem next) {
    shouldSetupNextUp(current, next, false);
  }

  @Override
  public void seekOccured(int reason) {
    shouldSetupNextUp(mVideoHandler.getCurrentVideo(), mVideoHandler.getNextVideo(), true);
  }

  private void shouldSetupNextUp(PlexVideoItem currentVideo, PlexVideoItem nextVideo, boolean force) {
    mHandler.removeCallbacks(mRunnable);
    if (mGlue.isPlaying()) {
      synchronized (this) {

        Log.d(Tag, "Next up released (setup)");
        if (nextVideo != null && currentVideo != null) {
          long time = currentVideo.getNextUpThresholdTrigger();
          long pos = mGlue.getCurrentPosition();
          if (shouldShow(pos, time, force)) {
            long delay = time - pos;
            Log.d(Tag, "Next up SET for : " + (delay / 1000) + " seconds");
            mHandler.postDelayed(mRunnable, delay);
          }
        }
      }
    }
  }

  private boolean shouldShow(long pos, long time, boolean force) {
    return time != NEXTUP_DISABLED &&
     (pos < time || (!mWasShown && force));
  }

  private class NextUpRunnable implements Runnable {

    private final SelectView.SelectViewCaller viewCaller;

    NextUpRunnable(SelectView.SelectViewCaller viewCaller) {
      this.viewCaller = viewCaller;
    }

    @Override
    public void run() {

      PlexVideoItem nextVideo = mVideoHandler.getNextVideo();
      if (nextVideo != null) {
        mFragment.hideControlsOverlay(true);
        Log.d(Tag,"Next up initiated");
        mWasShown = true;
        mUiFragmentNotifier.setView(new NextUpView(mFragment.getActivity(), mServer, nextVideo,
         mGlue.getDuration() - mGlue.getCurrentPosition(), NextUpHandler.this, viewCaller));
      }
    }

  }

}
