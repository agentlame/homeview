package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.util.HashSet;
import java.util.Set;

public class QueueChangeNotifier {

  public interface Observer {
    void queueChanged(PlexVideoItem previous, PlexVideoItem current, PlexVideoItem next);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void queueChanged(PlexVideoItem previous, PlexVideoItem current, PlexVideoItem next) {
    for(Observer obs : new HashSet<>(observers))
      obs.queueChanged(previous, current, next);
  }
}
