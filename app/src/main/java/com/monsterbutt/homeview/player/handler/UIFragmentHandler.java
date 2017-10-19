package com.monsterbutt.homeview.player.handler;


import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.view.KeyEvent;

import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.player.notifier.QueueChangeNotifier;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchChapterNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchTrackNotifier;
import com.monsterbutt.homeview.player.notifier.UIFragmentNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.android.SelectView;

public class UIFragmentHandler
 implements SelectView.SelectViewCaller, UIFragmentNotifier.Observer {

  public interface UIHandlerRegister {
    void register(PlaybackTransportControlGlue glue, UIFragmentHandler handler);
  }

  private final VideoFragment mFragment;

  private SelectView selectView = null;

  public UIFragmentHandler(VideoFragment fragment,
                           PlexServer server,
                           PlaybackTransportControlGlue glue,
                           VideoHandler videoHandler,
                           VideoChangedNotifier videoChangedNotifier,
                           QueueChangeNotifier queueChangeNotifier,
                           SeekOccuredNotifier seekOccuredNotifier,
                           SwitchTrackNotifier switchTrackNotifier,
                           SwitchChapterNotifier switchChapterNotifier,
                           ChapterSelectionNotifier chapterSelectionNotifier,
                           TrackSelector trackSelector) {
    mFragment = fragment;
    UIFragmentNotifier uiFragmentNotifier = new UIFragmentNotifier(this);
    new ResumeChoiceHandler(mFragment.getActivity(), glue, this,
     videoChangedNotifier, uiFragmentNotifier);
    new NextUpHandler(fragment, server, glue, videoHandler, this,
     videoChangedNotifier, queueChangeNotifier, seekOccuredNotifier, uiFragmentNotifier);
    new SwitchTrackHandler(fragment, server, this,
     uiFragmentNotifier, switchTrackNotifier, trackSelector);
    new SwitchChapterHandler(fragment.getActivity(), this,
     uiFragmentNotifier, switchChapterNotifier, chapterSelectionNotifier);
    if (mFragment.getActivity() instanceof UIHandlerRegister) {
      ((UIHandlerRegister) mFragment.getActivity()).register(glue, this);
    }
  }

  synchronized private boolean isFragmentUIUp() { return selectView != null; }

  @Override
  public synchronized boolean releaseSelectView(Class<?> viewClass) {
    if (selectView != null) {
      if (viewClass.isInstance(selectView)) {
        selectView.release();
        selectView = null;
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized void setView(SelectView view) {
    releaseSelectView(SelectView.class);
    selectView = view;
  }

  @Override
  public void selectionViewState(boolean isVisible, boolean shouldShowPlaybackUI) {

    if (shouldShowPlaybackUI) {
      mFragment.resetFocus();
      if (mFragment.isControlsOverlayAutoHideEnabled())
        mFragment.setControlsOverlayAutoHideEnabled(false);
      mFragment.setControlsOverlayAutoHideEnabled(true);
      mFragment.showControlsOverlay(true);
    }
    else
      mFragment.hideControlsOverlay(true);
    synchronized (this) {
      if (!isVisible && selectView != null && selectView.isReleased())
        selectView = null;
    }
  }

  public boolean shouldCancelOnBack() {
    return isFragmentUIUp() && releaseSelectView(SelectView.class);
  }

  public boolean handleKeyCode(int keyCode) {
    if (!isFragmentUIUp())
      return false;
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_DOWN:
      case KeyEvent.KEYCODE_DPAD_UP:
        if (isFragmentUIUp()) {
          releaseSelectView(SelectView.class);
          return true;
        }
        break;
    }
    return false;
  }

}
