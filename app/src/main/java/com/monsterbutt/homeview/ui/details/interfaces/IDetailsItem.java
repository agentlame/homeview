package com.monsterbutt.homeview.ui.details.interfaces;


import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public interface IDetailsItem {

  PlexServer server();
  PlexLibraryItem item();
  MediaTrackSelector tracks();
}
