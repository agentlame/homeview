package com.monsterbutt.homeview.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UILifecycleManager {

    public interface LifecycleListener {

        void onResume();
        void onPause();
        void onDestroyed();
    }

    private Map<String, LifecycleListener> mListeners = new HashMap<>();


    private List<LifecycleListener> getList() {

        List<LifecycleListener> listeners = new ArrayList<>();
        synchronized (this) {

            listeners.addAll(mListeners.values());
        }
        return listeners;
    }

    public void put(String key, LifecycleListener listener) {

        synchronized (this) {

            mListeners.put(key, listener);
        }
    }

    public void remove(String key) {

        synchronized (this) {

            if (mListeners.containsKey(key))
                mListeners.remove(key);
        }
    }

    public void resumed() {

        List<LifecycleListener> listeners = getList();
        for (LifecycleListener listener : listeners)
            listener.onResume();
    }

    public void paused() {

        List<LifecycleListener> listeners = getList();
        for (LifecycleListener listener : listeners)
            listener.onPause();
    }

    public void destroyed() {

    }
}
