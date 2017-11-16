package com.monsterbutt.homeview.ui.presenters;

import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;


public class CustomListRowPresenter extends ListRowPresenter {

  public interface Callback {
    RowsFragment getRowsFragment();
  }

  final private Callback callbackRowsFragment;
  final private HoverCard hoverPresenter = new HoverCard();

  public CustomListRowPresenter(Callback caller) {
    callbackRowsFragment = caller;
    PresenterSelector hover = new PresenterSelector() {
      @Override
      public Presenter getPresenter(Object item) {

        if (item instanceof SceneCardExpanded || item instanceof PosterCardExpanded)
          return hoverPresenter;
        return null;
      }
    };
    setHoverCardPresenterSelector(hover);
  }

  @Override
  protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
    if (callbackRowsFragment != null)
      callbackRowsFragment.getRowsFragment().setExpand(true);
    super.initializeRowViewHolder(holder);
  }
}
