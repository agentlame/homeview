package com.monsterbutt.homeview.ui.details.interfaces;


import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public interface IDetailsFragment {
  void setItem(PlexServer server, PlexLibraryItem item);
}
