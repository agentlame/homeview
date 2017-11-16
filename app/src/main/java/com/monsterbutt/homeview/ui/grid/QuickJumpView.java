package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpCallback;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpDisplay;

import java.util.List;

public class QuickJumpView implements IQuickJumpDisplay {

  private final Context context;
  private View quickJumpBar;
  private ListView quickList;
  private boolean mQuickListSelected = false;
  private int mQuickListSelectedIndex = 0;

  QuickJumpView(Activity activity, final IQuickJumpCallback callback) {
    this.context = activity;
    quickJumpBar = activity.findViewById(R.id.toolbarQuickJump);
    if (activity.getIntent().getBooleanExtra(C.EPISODEIST, false))
      setQuickListVisible(false);
    quickList = activity.findViewById(R.id.list_shortcut);
    quickList.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (v == quickList && !hasFocus) {
          mQuickListSelected = false;
        }
      }
    });
    quickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mQuickListSelected = true;
        callback.setSelectedPosition(((QuickJumpRow) quickList.getItemAtPosition(mQuickListSelectedIndex)).index);
      }
    });

    quickList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mQuickListSelectedIndex = position;
        if (mQuickListSelected)
          callback.setSelectedPosition(((QuickJumpRow) quickList.getItemAtPosition(position)).index);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  public void setQuickListVisible(boolean visible) {
    quickJumpBar.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  public void setQuickJumpList(List<QuickJumpRow> quickjumpList) {
    quickList.setAdapter(new ArrayAdapter<>(context, R.layout.quickjumprow, quickjumpList));
  }
}
