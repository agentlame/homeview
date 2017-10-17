package com.monsterbutt.homeview.player.notifier;


import java.util.HashSet;
import java.util.Set;

public class FrameRateSwitchNotifier {

  public interface Observer {
    void frameRateSwitched(long requestedDelay);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void frameRateSwitched(long requestedDelay) {
    for (Observer obs : new HashSet<>(observers))
      obs.frameRateSwitched(requestedDelay);
  }
}
