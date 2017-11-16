package com.monsterbutt.homeview.ui.interfaces;


import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.C;

public interface IRegisteredMedia {

  String getKey();
  String getParentKey();
  PlexLibraryItem.WatchedState getWatchedState();
  boolean updateStatus(C.StatusChanged status, int totalCount, int unwatchedCount);

  int getTotalLeaves();
  int getUnwatchedLeaves();

}
