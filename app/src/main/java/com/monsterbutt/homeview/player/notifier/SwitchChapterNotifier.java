package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.ui.PlexItemRow;

import java.util.HashSet;
import java.util.Set;

public class SwitchChapterNotifier {


  public interface Observer {
    void switchChapter(PlexItemRow row, int initialPosition);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void switchChapter(PlexItemRow row, int initialPosition) {
    for(Observer obs : new HashSet<>(observers))
      obs.switchChapter(row, initialPosition);
  }
}
