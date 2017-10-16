package com.monsterbutt.homeview.player.notifier;


import java.util.HashSet;
import java.util.Set;

public class PressStateNotifier {

  public enum PressState {
    FastForward,
    Rewind,
    Seek,
    Other
  }

  public interface Observer {
    void notify(PressState state);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void press(PressState state) {
    for(Observer obs : new HashSet<>(observers))
      obs.notify(state);
  }
}
