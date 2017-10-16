package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.plex.media.Chapter;

import java.util.HashSet;
import java.util.Set;

public class ChapterSelectionNotifier {

  public interface Observer {
    void chapterSelected(Chapter chapter);
  }

  private Set<Observer> observers = new HashSet<>();

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void chapterSelected(Chapter chapter) {
    for (Observer obs : new HashSet<>(observers))
      obs.chapterSelected(chapter);
  }
}
