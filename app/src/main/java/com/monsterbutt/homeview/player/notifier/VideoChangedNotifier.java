package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.handler.StartPositionHandler;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.util.HashSet;
import java.util.Set;

public class VideoChangedNotifier {

  public interface Observer {
    void notify(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                StartPositionHandler startPosition);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void finish() {
    videoChanged(null, null, true, null);
  }

  public void videoChanged(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                           StartPositionHandler startPosition) {
    for(Observer obs : new HashSet<>(observers))
      obs.notify(item, tracks, usesDefaultTracks, startPosition);
  }
}
