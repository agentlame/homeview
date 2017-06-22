package com.monsterbutt.homeview.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.monsterbutt.homeview.ui.PlexItemRow;


@SuppressLint("ValidFragment")
public class SelectionFragment extends DetailsFragment {

  private final ArrayObjectAdapter mAdapter;
  private final PlexItemRow row;
  private final int height;
  private final int initialPosition;

  public SelectionFragment(PlexItemRow row, OnItemViewClickedListener listener,
                            int initialPosition, int height) {

    this.row = row;
    this.height = height;
    this.initialPosition = initialPosition;

    ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
    presenterSelector.addClassPresenter(ListRow.class, new CustomListRowPresenter());
    setOnItemViewClickedListener(listener);
    mAdapter = new ArrayObjectAdapter(presenterSelector);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {

    super.onActivityCreated(savedInstanceState);

    setAdapter(mAdapter);
    mAdapter.add(row);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (view != null) {
      ViewGroup.LayoutParams lp = view.getLayoutParams();
      lp.height = height;
      view.setLayoutParams(lp);
    }
    return view;
  }

  private class CustomListRowPresenter extends ListRowPresenter {

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
      super.onBindRowViewHolder(holder, item);
      ListRowPresenter.SelectItemViewHolderTask task = new ListRowPresenter.SelectItemViewHolderTask(initialPosition);
      task.setSmoothScroll(true);
      task.run(holder);
    }
  }
}
