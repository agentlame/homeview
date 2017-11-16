package com.monsterbutt.homeview.ui.grid.interfaces;


import android.content.Context;

public interface IGridSorter {

  enum ItemSort {

    DateAdded,
    Duration,
    LastViewed,
    Rating,
    ReleaseDate,
    Title
  }

  void sort(Context context, ItemSort sortType, boolean ascending);
}
