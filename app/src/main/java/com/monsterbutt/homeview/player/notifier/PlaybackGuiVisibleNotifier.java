package com.monsterbutt.homeview.player.notifier;


import java.util.HashSet;
import java.util.Set;

public class PlaybackGuiVisibleNotifier {

  public interface Observer {
    void notify(boolean visible);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void visibleGUI(boolean visible) {
    for (Observer obs : new HashSet<>(observers))
      obs.notify(visible);
  }
}
