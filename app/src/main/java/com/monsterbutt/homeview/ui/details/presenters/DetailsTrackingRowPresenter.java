package com.monsterbutt.homeview.ui.details.presenters;


import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;

import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRow;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRowListener;

public class DetailsTrackingRowPresenter extends ListRowPresenter implements IDetailsScrollRowListener {

  private RowPresenter.ViewHolder holder;
  private final String key;

  public DetailsTrackingRowPresenter(String key) {
    this.key = key;
  }

  @Override
  protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
    super.onBindRowViewHolder(holder, item);
    this.holder = holder;
    if (!(item instanceof IDetailsScrollRow) || ((ListRow) item).getAdapter().size() == 0)
      return;
    scrollToIndex(((IDetailsScrollRow) item).getCurrentIndex());
  }

  @Override
  public void scrollToIndex(int index) {
    if (0 > index || holder == null)
      return;
    SelectItemViewHolderTask task = new SelectItemViewHolderTask(index);
    task.setSmoothScroll(true);
    task.run(holder);
  }

  @Override
  public String getKey() {
    return key;
  }
}
