package com.monsterbutt.homeview.ui;

import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UILifecycleManager {

  private enum State {
      Active,
      Paused,
      Destroyed
  }

  private Map<String, ILifecycleListener> listeners = new HashMap<>();

  private List<ILifecycleListener> getList() {
    List<ILifecycleListener> list = new ArrayList<>();
    synchronized (this) {
      list.addAll(listeners.values());
    }
    return list;
  }

  public void register(String key, ILifecycleListener listener) {
    synchronized (this) {
      listeners.put(key, listener);
    }
  }

  public void unregister(String key) {
    synchronized (this) {
      if (listeners.containsKey(key))
        listeners.remove(key);
    }
  }

  public void resumed() {
    for (ILifecycleListener listener : getList())
      listener.onResume();
  }

  public void paused() {
    for (ILifecycleListener listener : getList())
      listener.onPause();
  }

  public void destroyed() {
    for (ILifecycleListener listener : getList())
      listener.onDestroyed();
  }

}
