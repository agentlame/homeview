package com.monsterbutt.homeview.ui.details.interfaces;


import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

public interface IDetailObject {

  PlexLibraryItem item();
  MediaTrackSelector tracks();
}
