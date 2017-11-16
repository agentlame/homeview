package com.monsterbutt.homeview.player.notifier;


import android.support.v17.leanback.widget.ListRow;


import java.util.HashSet;
import java.util.Set;

public class SwitchChapterNotifier {


  private Set<Observer> observers = new HashSet<>();

  public interface Observer {
    void switchChapter(ListRow row, int initialPosition);
  }

  public void register(Observer obs) {
    observers.add(obs);
  }

  public void unregister(Observer obs) {
    observers.remove(obs);
  }

  public void switchChapter(ListRow row, int initialPosition) {
    for(Observer obs : new HashSet<>(observers))
      obs.switchChapter(row, initialPosition);
  }
}
