package com.monsterbutt.homeview.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.PlexItemRow;


@SuppressLint("ValidFragment")
public class SelectionFragment extends DetailsFragment {

  private final ArrayObjectAdapter mAdapter;
  private final PlexItemRow row;
  private final Activity activity;
  private final int height;

  public SelectionFragment(Activity activity, PlexItemRow row, OnItemViewClickedListener listener,
                            int height) {

    this.row = row;
    this.activity = activity;
    this.height = height;

    ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
    presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
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
}
