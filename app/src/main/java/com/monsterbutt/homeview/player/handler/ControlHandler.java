package com.monsterbutt.homeview.player.handler;

import android.app.Activity;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.view.KeyEvent;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.notifier.PlaybackGuiVisibleNotifier;
import com.monsterbutt.homeview.player.notifier.PressStateNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

public class ControlHandler implements PlaybackGuiVisibleNotifier.Observer,
 VideoChangedNotifier.Observer {

  private final Activity mActivity;
  private final PlaybackTransportControlGlue mGlue;
  private final VideoChangedNotifier mVideoChangedNotifier;
  private View mSeekBar;
  private int mSeekSeconds = 60;

  private boolean isGuiVisible = false;

  public ControlHandler(Activity activity, PlaybackTransportControlGlue glue,
                        VideoChangedNotifier videoChangedNotifier,
                        PlaybackGuiVisibleNotifier playbackGuiVisibleNotifier) {
    mActivity = activity;
    mGlue = glue;
    mSeekBar = activity.findViewById(R.id.playback_progress);
    mVideoChangedNotifier = videoChangedNotifier;
    if (videoChangedNotifier != null)
      videoChangedNotifier.register(this);
    if (playbackGuiVisibleNotifier != null)
      playbackGuiVisibleNotifier.register(this);
  }

  public boolean handleKeyCode(int keyCode, PressStateNotifier pressState) {
    switch (keyCode) {

      case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
        mGlue.pause();
        jumpTime(1);
        return true;
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        jumpForward();
        return true;
      case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
        mGlue.next();
        return true;

      case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
        mGlue.pause();
        jumpTime(-1);
        return true;
      case KeyEvent.KEYCODE_MEDIA_REWIND:
        jumpBack();
        return true;
      case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
        mGlue.previous();
        return true;

      case KeyEvent.KEYCODE_MEDIA_STOP:
        if (mGlue.isPlaying())
          mGlue.pause();
        finishPlayback();
        return true;

      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        mGlue.pause();
        return true;

      case KeyEvent.KEYCODE_MEDIA_PLAY:
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        if (mGlue.isPlaying())
          mGlue.pause();
        else
          mGlue.play();
        return true;

      case KeyEvent.KEYCODE_DPAD_UP:
        if (!isGuiVisible) {
          pressState.press(PressStateNotifier.PressState.Seek);
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_DPAD_LEFT:

        if (isSeekBarFocused()) {
          jumpTime(-mSeekSeconds);
          return true;
        } else if (!isGuiVisible) {
          jumpBack();
          pressState.press(PressStateNotifier.PressState.Rewind);
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_DPAD_RIGHT:

        if (isSeekBarFocused()) {
          jumpTime(mSeekSeconds);
          return true;
        } else if (!isGuiVisible) {
          pressState.press(PressStateNotifier.PressState.FastForward);
          jumpForward();
          return true;
        }
        return false;

      default:
        return false;
    }
  }

  public void finishPlayback() {
    if (mVideoChangedNotifier != null)
      mVideoChangedNotifier.finish();
  }

  public void jumpBack() {
    jumpTime(-1 * Long.parseLong(mGlue.getContext().getString(R.string.skip_back_seconds)));
  }

  public void jumpForward() {
    jumpTime(Long.parseLong(mGlue.getContext().getString(R.string.skip_forward_seconds)));
  }

  private void jumpTime(long seconds) {

    long pos = mGlue.getCurrentPosition() + (seconds * 1000);
    if (pos < 0)
      pos = 0;
    else if (pos > mGlue.getDuration())
      pos = mGlue.getDuration();
    mGlue.seekTo(pos);
  }

  private boolean isSeekBarFocused() {
    if (mSeekBar == null)
      mSeekBar = mActivity.findViewById(R.id.playback_progress);

    return mSeekBar != null && mSeekBar.isFocused();
  }

  @Override
  public void notify(boolean visible) {
    isGuiVisible = visible;
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks,
                     boolean usesDefaultTracks, StartPositionHandler startPosition) {
    if (item == null)
      return;

    final int fiveMinutes = 5 * 60;
    // break up into increaments of 20
    int seconds = 60 * (int) item.getDurationInMin();
    if ((seconds / 20) > fiveMinutes)
      mSeekSeconds = fiveMinutes;
    else if ((seconds / 20) < 15)
      mSeekSeconds = 15;
    else
      mSeekSeconds = seconds / 20;
  }
}
