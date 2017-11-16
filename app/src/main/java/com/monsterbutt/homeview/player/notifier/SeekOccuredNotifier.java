package com.monsterbutt.homeview.player.notifier;


import java.util.HashSet;
import java.util.Set;

public class SeekOccuredNotifier {

  public interface Observer {
    void seekOccured(int reason);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void seekOccured(int reason) {
    for(Observer obs : new HashSet<>(observers))
      obs.seekOccured(reason);
  }
}
