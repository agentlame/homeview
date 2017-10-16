package com.monsterbutt.homeview.ui.android;


import android.app.Activity;
import android.os.Handler;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.ResumeChoiceCard;
import com.monsterbutt.homeview.ui.PlexItemRow;

public class ResumeChoiceView extends SelectViewRow {

  private static final int CHOOSER_TIMEOUT = 15 * 1000;

  private final PlaybackTransportControlGlue playbackHandler;
  private final ChoiceTimeout runnable;
  private final Handler handler;

  private ResumeChoiceView(Activity activity, PlaybackTransportControlGlue playbackHandler) {
    super(activity);
    this.playbackHandler = playbackHandler;
    runnable = new ChoiceTimeout();
    handler = new Handler();
    handler.postDelayed(runnable, CHOOSER_TIMEOUT);
  }

  public static ResumeChoiceView getView(Activity activity, PlaybackTransportControlGlue playbackHandler, long offset,
                                      SelectView.SelectViewCaller caller) {
    ResumeChoiceView selectView = new ResumeChoiceView(activity, playbackHandler);
    selectView.setRow(PlexItemRow.buildResumeChoices(activity, activity.getString(R.string.playback_start_dialog), offset), 0,caller);
    return selectView;
  }

  @Override
  protected String getTag() {
    return "resumeChoice";
  }

  @Override
  protected int getHeight() {
    return 600;
  }

  @Override
  protected void cardClicked(PosterCard card) {

    handler.removeCallbacks(runnable);
    if (card instanceof ResumeChoiceCard) {
      long offset = ((ResumeChoiceCard) card).offset;
      if (offset > 0)
        playbackHandler.seekTo(offset);
    }
  }

  @Override
  protected boolean showPlaybackUIOnFragmentSet() { return true; }

  @Override
  protected boolean shouldShowPlayerUIOnRelease() { return true; }

  private class ChoiceTimeout implements Runnable {

    @Override
    public void run() {
      ResumeChoiceView.this.release();
    }
  }
}
