package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridSorter;

import java.util.ArrayList;
import java.util.List;

class SectionSortView {

  private SectionFilterArrayAdapter mSorts;
  private TextView mSortText;
  private final Activity activity;
  private final IGridSorter gridSorter;

  SectionSortView(Activity activity, View view, IGridSorter gridSorter, boolean hide) {
    this.activity = activity;
    this.gridSorter = gridSorter;
    List<SectionFilter> list = new ArrayList<>();
    list.add(new SectionSort(activity.getString(R.string.sort_DateAdded), IGridSorter.ItemSort.DateAdded));
    list.add(new SectionSort(activity.getString(R.string.sort_Duration), IGridSorter.ItemSort.Duration));
    list.add(new SectionSort(activity.getString(R.string.sort_LastViewed), IGridSorter.ItemSort.LastViewed));
    list.add(new SectionSort(activity.getString(R.string.sort_Rating), IGridSorter.ItemSort.Rating));
    list.add(new SectionSort(activity.getString(R.string.sort_ReleaseDate), IGridSorter.ItemSort.ReleaseDate));
    list.add(new SectionSort(activity.getString(R.string.sort_Title), IGridSorter.ItemSort.Title));
    mSorts = new SectionFilterArrayAdapter(activity, list, list.get(list.size()-1));

    Button sortBtn = view.findViewById(R.id.sortBtn);
    sortBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        sortButtonClicked();
      }
    });
    mSortText = view.findViewById(R.id.sortText);
    mSortText.setText(mSorts.selected().name);

    if (hide) {
      sortBtn.setVisibility(View.GONE);
      mSortText.setVisibility(View.GONE);
    }
  }

  private void sortButtonClicked() {

    new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
     .setIcon(R.drawable.launcher)
     .setTitle(R.string.sort_title)
     .setAdapter(mSorts, new DialogInterface.OnClickListener() {
       @Override
       public void onClick(DialogInterface dialog, int which) {

         SectionSort oldSelected = (SectionSort) mSorts.selected();
         boolean isAscending = oldSelected.isAscending;
         IGridSorter.ItemSort lastSort = oldSelected.id;
         SectionSort selected = (SectionSort) mSorts.selected(which);
         if (lastSort == selected.id) {
           isAscending = !isAscending;
           selected.isAscending = isAscending;
         }

         mSortText.setText(selected.name);
         gridSorter.sort(activity, selected.id, isAscending);
         dialog.dismiss();
       }
     })
     .create()
     .show();
  }
}
