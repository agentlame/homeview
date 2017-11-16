package com.monsterbutt.homeview.ui.playback.views;


import android.app.Activity;
import android.os.Handler;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.presenters.ResumeChoiceCard;
import com.monsterbutt.homeview.ui.presenters.ResumeChoicePresenter;

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

    ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ResumeChoicePresenter());
    adapter.add(new ResumeChoiceCard(activity, activity.getString(R.string.playback_start_dialog_resume),
     offset, activity.getDrawable(R.drawable.ic_slow_motion_video_white_48dp)));
    adapter.add(new ResumeChoiceCard(activity, activity.getString(R.string.playback_start_dialog_begin),
     0, activity.getDrawable(R.drawable.ic_play_circle_outline_white_48dp)));
    selectView.setRow(new ListRow(new HeaderItem(activity.getString(R.string.playback_start_dialog)), adapter), 0,caller);
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
