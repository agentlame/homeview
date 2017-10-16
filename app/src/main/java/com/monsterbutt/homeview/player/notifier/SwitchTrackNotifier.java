package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.player.track.MediaTrackSelector;

import java.util.HashSet;
import java.util.Set;

public class SwitchTrackNotifier {

  public interface Observer {
    void switchTrackSelect(int streamType, MediaTrackSelector tracks);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void switchTrack(int streamType, MediaTrackSelector tracks) {
    for(Observer obs : new HashSet<>(observers))
      obs.switchTrackSelect(streamType, tracks);
  }
}
