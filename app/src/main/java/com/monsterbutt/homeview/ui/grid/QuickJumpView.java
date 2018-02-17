package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpCallback;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpDisplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickJumpView implements IQuickJumpDisplay {

  private enum FocusChanging {
    No,
    Changing,
    Finishing
  }

  private final Context context;
  private View quickJumpBar;
  private ListView quickList;
  private Map<String, Integer> mMap = new HashMap<>();
  private boolean mQuickListSelected = false;
  private int mQuickListSelectedIndex = 0;
  private FocusChanging mFocusChanging = FocusChanging.No;

  QuickJumpView(Activity activity, final IQuickJumpCallback callback) {
    this.context = activity;
    quickJumpBar = activity.findViewById(R.id.toolbarQuickJump);
    if (activity.getIntent().getBooleanExtra(C.EPISODEIST, false))
      setQuickListVisible(false);
    quickList = activity.findViewById(R.id.list_shortcut);
    quickList.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (v == quickList) {
          if (hasFocus) {
            mFocusChanging = FocusChanging.Changing;
          }
          mQuickListSelected = hasFocus;
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
        if (mFocusChanging == FocusChanging.Changing) {
          mFocusChanging = FocusChanging.Finishing;
          quickList.setSelection(mQuickListSelectedIndex);
          return;
        }
        else
          mQuickListSelectedIndex = position;

        view.setActivated(true);
        if (mFocusChanging != FocusChanging.Finishing) {
          if (mQuickListSelected)
            callback.setSelectedPosition(((QuickJumpRow) quickList.getItemAtPosition(position)).index);
        }
        else
          mFocusChanging = FocusChanging.No;
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  public void setQuickListVisible(boolean visible) {
    quickJumpBar.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  @Override
  public void setQuickJumpList(List<QuickJumpRow> quickjumpList) {
    mMap.clear();
    int index = 0;
    for (QuickJumpRow row : quickjumpList) {
      mMap.put(row.letter.substring(0, 1).toUpperCase(), index++);
    }
    quickList.setAdapter(new ArrayAdapter<>(context, R.layout.quickjumprow, quickjumpList));
    if (!quickjumpList.isEmpty())
      quickList.setSelection(0);
  }

  @Override
  public void setCurrentSelectionName(String name) {
    if (TextUtils.isEmpty(name))
      return;
    String letter = name.substring(0, 1).toUpperCase();
    if (Character.isDigit(letter.charAt(0)))
      letter = "#";
    Integer index = mMap.get(letter);
    if (index != null && index != mQuickListSelectedIndex) {
      quickList.setSelection(index);
    }
  }
}
