package com.monsterbutt.homeview.player;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.media.PlaybackBaseControlGlue;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v17.leanback.media.PlayerAdapter;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.monsterbutt.homeview.player.display.ScreenLock;
import com.monsterbutt.homeview.player.handler.ControlHandler;
import com.monsterbutt.homeview.player.handler.PlayStatusHandler;
import com.monsterbutt.homeview.player.handler.SelectActionHandler;
import com.monsterbutt.homeview.player.handler.UIFragmentHandler;
import com.monsterbutt.homeview.player.handler.VideoHandler;
import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.player.notifier.PlaybackGuiVisibleNotifier;
import com.monsterbutt.homeview.player.notifier.PressStateNotifier;
import com.monsterbutt.homeview.player.notifier.QueueChangeNotifier;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchChapterNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchTrackNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.presenters.PlaybackTransportRowPresenter;

import static com.monsterbutt.homeview.plex.media.PlexVideoItem.BAD_CHAPTER_START;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.START_CHAPTER_THRESHOLD;

/**
 * PlayerGlue for video playback
 * @param <T>
 */
public class VideoMediaPlayerGlue<T extends PlayerAdapter> extends PlaybackTransportControlGlue<T> {

  public static final String TAG = "HV_VideoMediaPlayerGlue";
  private static final int NUM_EXTRA_PLAYBACK_SPEEDS = 1;

  private final PlaybackControlsRow.SkipPreviousAction mPreviousAction;
  private final PlaybackControlsRow.RewindAction mRewindAction;
  private final PlaybackControlsRow.FastForwardAction mFastForwardAction;
  private final PlaybackControlsRow.SkipNextAction mNextAction;

  private final VideoHandler mVideoHandler;
  private final ControlHandler mControlHandler;
  private final UIFragmentHandler mUIHandler;
  private final SelectActionHandler mSelectActionHandler;
  private final PlayStatusHandler mPlayStatusHandler;

  private final PlexServer mServer;

  private final ScreenLock mScreenLock;
  private boolean wasPausedAfterPlayback = false;

  private PressStateNotifier mPressState = new PressStateNotifier();

  public VideoMediaPlayerGlue(VideoFragment fragment, PlexServer server, ScreenLock screenLock, T impl,
                              VideoChangedNotifier videoChangedNotifier, SeekOccuredNotifier seekOccuredNotifier,
                              PlaybackGuiVisibleNotifier playbackGuiVisibleNotifier,
                              TrackSelector trackSelector) {
    super(fragment.getContext(), impl);

    mScreenLock = screenLock;
    mServer = server;

    Context context = fragment.getContext();

    mPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
    mRewindAction = new PlaybackControlsRow.RewindAction(context, NUM_EXTRA_PLAYBACK_SPEEDS);
    mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context, NUM_EXTRA_PLAYBACK_SPEEDS);
    mNextAction = new PlaybackControlsRow.SkipNextAction(context);

    SwitchTrackNotifier switchTrackNotifier = new SwitchTrackNotifier();
    SwitchChapterNotifier switchChapterNotifier = new SwitchChapterNotifier();
    QueueChangeNotifier queueChangeNotifier = new QueueChangeNotifier();
    ChapterSelectionNotifier chapterSelectionNotifier = new ChapterSelectionNotifier();

    mSelectActionHandler = new SelectActionHandler(fragment.getActivity(), server, this,
     videoChangedNotifier, switchTrackNotifier, switchChapterNotifier);
    mVideoHandler = new VideoHandler(fragment, this, mServer,
     videoChangedNotifier, queueChangeNotifier, chapterSelectionNotifier);
    mControlHandler = new ControlHandler(fragment.getActivity(), this, videoChangedNotifier,
     playbackGuiVisibleNotifier);
    mUIHandler = new UIFragmentHandler(fragment, mServer, this, mVideoHandler,
     videoChangedNotifier, queueChangeNotifier, seekOccuredNotifier,
     switchTrackNotifier, switchChapterNotifier, chapterSelectionNotifier, trackSelector);
    mPlayStatusHandler = new PlayStatusHandler(fragment, server, this,
     videoChangedNotifier, seekOccuredNotifier);
  }

  @Override
  protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
    mSelectActionHandler.onCreateActions(adapter);
  }

  @Override
  protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
    adapter.add(mPreviousAction);
    adapter.add(mRewindAction);
    super.onCreatePrimaryActions(adapter);
    adapter.add(mFastForwardAction);
    adapter.add(mNextAction);
  }

  @Override
  public void onActionClicked(Action action) {
    if (shouldDispatchAction(action)) {
      dispatchAction(action);
      return;
    }
    super.onActionClicked(action);
  }

  private boolean shouldDispatchAction(Action action) {
    return action == mFastForwardAction || action == mRewindAction ||
     action == mNextAction || action == mPreviousAction ||
     mSelectActionHandler.shouldDispatchAction(action);
  }

  private void dispatchAction(Action action) {
    if (action == mFastForwardAction) {
      mControlHandler.jumpForward();
    } else if (action == mRewindAction) {
      mControlHandler.jumpBack();
    } else if (action == mNextAction) {
      next();
    } else if (action == mPreviousAction) {
      previous();
    } else {
      mSelectActionHandler.dispatchAction(action);
    }
  }

  @Override
  protected void onPlayCompleted() {
    super.onPlayCompleted();
    new Handler().post(new Runnable() {
      @Override
      public void run() {
        if (!mVideoHandler.playNext())
          mControlHandler.finishPlayback();
      }
    });
  }

  public boolean parseIntent(Intent intent) {
    return mVideoHandler.parseIntent(intent);
  }

  public void setRefreshRateToCurrentVideo(Activity activity) {

    if (!wasPausedAfterPlayback)
      return;
    VideoHandler.setRefreshRateToCurrentVideo(activity, mVideoHandler.getCurrentVideo(), true, null);
  }

  @Override
  public void play() {
    if(!isPrepared()) {
      Log.w(TAG,"Not prepared to play yet, canceling");
      return;
    }
    mScreenLock.obtain();
    mVideoHandler.shouldDoFirstPlay();
    wasPausedAfterPlayback = false;

    super.play();
  }

  @Override
  public void pause() {
    wasPausedAfterPlayback = true;
    mScreenLock.release();

    super.pause();
  }

  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    Log.d(TAG, "PIP Mode changed, is in PIP: " + isInPictureInPictureMode);
    mServer.isPIPActive(isInPictureInPictureMode);
  }

  @Override
  public void previous() {
    if (mVideoHandler.hasVideo()) {
      long position = mVideoHandler.getPreviousChapterPosition(getCurrentPosition());
      if (position != BAD_CHAPTER_START && getCurrentPosition() > START_CHAPTER_THRESHOLD)
        seekTo(position);
      else if (!mVideoHandler.playPrevious())
        seekTo(0);
    }
  }

  @Override
  public void next() {
    if (mVideoHandler.hasVideo()) {
      long position = mVideoHandler.getNextChapterPosition(getCurrentPosition());
      if (position != BAD_CHAPTER_START)
        seekTo(position);
      else if (!mVideoHandler.playNext())
        mControlHandler.finishPlayback();
    }
  }

  @Override
  public boolean onKey(View v, int keyCode, KeyEvent event) {
    if (event.getAction() != KeyEvent.ACTION_DOWN) {
      return mUIHandler.handleKeyCode(keyCode) || super.onKey(v, keyCode, event);
    }

    mPressState.press(PressStateNotifier.PressState.Other);
    return mUIHandler.handleKeyCode(keyCode) ||
     mControlHandler.handleKeyCode(keyCode, mPressState) ||
     super.onKey(v, keyCode, event);
  }

  @Override
  protected PlaybackRowPresenter onCreateRowPresenter() {
    final AbstractDetailsDescriptionPresenter detailsPresenter =
     new AbstractDetailsDescriptionPresenter() {
       @Override
       protected void onBindDescription(ViewHolder
                                         viewHolder, Object obj) {
         PlaybackBaseControlGlue glue = (PlaybackBaseControlGlue) obj;
         viewHolder.getTitle().setText(glue.getTitle());
         viewHolder.getSubtitle().setText(glue.getSubtitle());
       }
     };

    PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
      @Override
      protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);
        vh.setOnKeyListener(VideoMediaPlayerGlue.this);
      }
      @Override
      protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
        super.onUnbindRowViewHolder(vh);
        vh.setOnKeyListener(null);
      }
    };
    mPressState.register(rowPresenter);
    rowPresenter.setDescriptionPresenter(detailsPresenter);
    return rowPresenter;
  }

  public void release() {
    mPlayStatusHandler.release();
  }

}
