package com.monsterbutt.homeview.ui.playback.views;


import android.app.Activity;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;

import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.playback.SelectionFragment;

public abstract class SelectViewRow extends SelectView
 implements OnItemViewClickedListener {

  SelectViewRow(Activity activity) {
    super(activity);
  }

  protected abstract void cardClicked(PosterCard card);

  public void setRow(ListRow row, int initialPosition, SelectViewCaller caller) {
    setFragment(new SelectionFragment(row, this, initialPosition, getHeight()), caller);
  }

  private void clicked(PosterCard card) {
    cardClicked(card);
    release();
  }

  @Override
  public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
    clicked((PosterCard) item);
  }

  @Override
  protected boolean showPlaybackUIOnFragmentSet() { return false; }
}
