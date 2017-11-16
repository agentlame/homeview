package com.monsterbutt.homeview.ui.details.presenters;


import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;

import com.monsterbutt.homeview.ui.details.interfaces.IDetailTrackingRow;

public class DetailsTrackingRowPresenter extends ListRowPresenter {

  @Override
  protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
    super.onBindRowViewHolder(holder, item);
    if (!(item instanceof IDetailTrackingRow) || ((ListRow) item).getAdapter().size() == 0)
      return;
    int startIndex = ((IDetailTrackingRow) item).getCurrentIndex();
    if (0 > startIndex)
      return;
    SelectItemViewHolderTask task =
     new SelectItemViewHolderTask(startIndex);
    task.setSmoothScroll(true);
    task.run(holder);
  }
}
