package com.monsterbutt.homeview.player.notifier;


import com.monsterbutt.homeview.ui.android.SelectView;

public class UIFragmentNotifier {

  public interface Observer {
    void setView(SelectView view);
    boolean releaseSelectView(Class<?> viewClass);
  }

  private final Observer mObserver;

  public UIFragmentNotifier(Observer obs) { mObserver = obs; }

  public void setView(SelectView view) { mObserver.setView(view); }

  public boolean releaseSelectView(Class<?> viewClass) { return mObserver.releaseSelectView(viewClass);}
}
