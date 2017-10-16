package com.monsterbutt.homeview.presenters;

import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.notifier.PressStateNotifier;

import static com.monsterbutt.homeview.player.notifier.PressStateNotifier.PressState.Seek;

public class PlaybackTransportRowPresenter extends
 android.support.v17.leanback.widget.PlaybackTransportRowPresenter
 implements PressStateNotifier.Observer {

  private final static int REWIND_INDEX = 1;
  private final static int PLAY_PAUSE_INDEX = 2;
  private final static int FASTFORWARD_INDEX = 3;

  private PressStateNotifier.PressState mPressState = PressStateNotifier.PressState.Other;
  private View mPlayPauseAction = null;
  private View mRewindAction = null;
  private View mFastForwardAction = null;

  private ViewGroup mControlsDock;

  @Override
  public void onReappear(RowPresenter.ViewHolder rowViewHolder) {

    ViewHolder vh = (ViewHolder) rowViewHolder;
    if (getPressState() != Seek && vh.view.hasFocus()) {

      int desiredIndex = getDesiredIndex();
      switch (desiredIndex) {
        case FASTFORWARD_INDEX:
          if (mFastForwardAction != null) {
            mFastForwardAction.requestFocus();
            return;
          }
          break;
        case REWIND_INDEX:
          if (mRewindAction != null) {
            mRewindAction.requestFocus();
            return;
          }
          break;
        case PLAY_PAUSE_INDEX:
        default:
          if (mPlayPauseAction != null) {
            mPlayPauseAction.requestFocus();
            return;
          }
          break;
      }
      if (mControlsDock != null && checkForPlayButton(mControlsDock, desiredIndex)) {
        onReappear(rowViewHolder);
        return;
      }
    }
    super.onReappear(rowViewHolder);
  }

  private boolean checkForPlayButton(ViewGroup group, int desiredIndex) {
    int count = group.getChildCount();
    for (int i = 0; i < count; ++i) {
      View v = group.getChildAt(i);
      // this is a terrible hack but ID doesn't seem to be filled in a view
      if (v instanceof FrameLayout && i == desiredIndex) {
        switch(desiredIndex) {
          case FASTFORWARD_INDEX:
            mFastForwardAction = v;
            break;
          case REWIND_INDEX:
            mRewindAction = v;
            break;
          case PLAY_PAUSE_INDEX:
            mPlayPauseAction = v;
            break;
        }
        return true;
      }
      else if (v instanceof ViewGroup) {
        if (checkForPlayButton((ViewGroup) v, desiredIndex))
          return true;
      }
    }
    return false;
  }

  private int getDesiredIndex() {
    switch(getPressState()) {
      case Rewind:
        return REWIND_INDEX;
      case FastForward:
        return FASTFORWARD_INDEX;
      case Other:
      default:
        return PLAY_PAUSE_INDEX;
    }
  }

  private PressStateNotifier.PressState getPressState() {
    return mPressState;
  }

  @Override
  protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
    ViewHolder vh = (ViewHolder) super.createRowViewHolder(parent);
    mControlsDock = vh.view.findViewById(android.support.v17.leanback.R.id.controls_dock);
    mPlayPauseAction = vh.view.findViewById(R.id.lb_control_play_pause);
    return vh;
  }

  @Override
  public void notify(PressStateNotifier.PressState state) {
    mPressState = state;
  }
}
