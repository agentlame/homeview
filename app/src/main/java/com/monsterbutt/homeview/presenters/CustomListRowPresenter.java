package com.monsterbutt.homeview.presenters;

import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;


public class CustomListRowPresenter extends ListRowPresenter {

  public interface Callback {
    RowsFragment getRowsFragment();
  }

  final private Callback callback;
  final private HoverCard hoverPresenter = new HoverCard();
  final PresenterSelector hover = new PresenterSelector() {
    @Override
    public Presenter getPresenter(Object item) {

      if (item instanceof SceneCardExpanded || item instanceof PosterCardExpanded)
        return hoverPresenter;
      return null;
    }
  };

  public CustomListRowPresenter(Callback caller) {
    callback = caller;
    setHoverCardPresenterSelector(hover);
  }

  @Override
  protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
    if (callback != null)
      callback.getRowsFragment().setExpand(true);
    super.initializeRowViewHolder(holder);
  }
}
