package com.monsterbutt.homeview.ui.interfaces;


import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public interface IMediaObserver {
  void statusChanged(IRegisteredMedia media, PlexLibraryItem.WatchedState status);
}
