package com.monsterbutt.homeview.plex;


import android.support.annotation.NonNull;
import android.text.TextUtils;


import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.interfaces.IMediaObserver;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StatusWatcher {

  private ObserversMap map = new ObserversMap();

  public StatusWatcherObserver registerObserver(@NonNull IMediaObserver observer) {
    return new StatusWatcherObserver(observer);
  }

  public class StatusWatcherObserver {

    private final IMediaObserver observer;

    StatusWatcherObserver(@NonNull IMediaObserver observer) {
      this.observer = observer;
    }

    public void attach(@NonNull Collection<IRegisteredMedia> media) {
      for (IRegisteredMedia item : media) {
        MediaEntry entry = map.get(item.getKey());
        if (entry == null) {
          entry = new MediaEntry(item, map);
          map.put(item.getKey(), entry);
        }
        entry.attach(new Observer(observer, item), map);
      }
    }

    public void release(@NonNull Collection<IRegisteredMedia> media) {
      for (IRegisteredMedia item : media) {
        MediaEntry entry = map.get(item.getKey());
        if (entry != null)
          entry.release(observer);
      }
    }
  }

  public void changeStatus(String key, PlexLibraryItem.WatchedState status) {
    MediaEntry entry = map.get(key);
    if (entry != null)
      entry.changeStatus(status);
  }

  private class Observer {

    final IMediaObserver observer;
    final IRegisteredMedia media;

    Observer(IMediaObserver observer, IRegisteredMedia media) {
      this.observer = observer;
      this.media = media;
    }
  }


  private class MediaEntry  {

    final String key;

    private MediaEntry parent;

    private IRegisteredMedia media;

    private Map<String, MediaEntry> children = new HashMap<>();
    private Map<IMediaObserver, Observer> observers = new HashMap<>();

    MediaEntry(IRegisteredMedia media, ObserversMap map) {
      this(media.getKey());
      addParent(media.getParentKey(), map);
    }

    MediaEntry(String key) {
      this.key = key;
    }

    private void addParent(String parentKey, ObserversMap map) {
      if (!TextUtils.isEmpty(parentKey)) {
        if (parent == null) {
          parent = new MediaEntry(parentKey);
          map.put(parentKey, parent);
        }
        parent.children.put(key, this);
      }
    }

    void attach(Observer observer, ObserversMap map) {

      if (media != null) {
        notifyObservers(PlexLibraryItem.WatchedState.Refreshed,
         observer.media.getTotalLeaves(), observer.media.getUnwatchedLeaves());
      }
      else
        media = observer.media;
      addParent(media.getParentKey(), map);
      observers.put(observer.observer, observer);
    }

    void release(IMediaObserver observer) {
      observers.remove(observer);
      if (observers.isEmpty() && parent != null) {
        parent.children.remove(media.getKey());
      }
    }

    private void notifyObservers(PlexLibraryItem.WatchedState status, int totalCount, int unwatchedCount) {
      if (media.updateStatus(status, totalCount, unwatchedCount)) {
        for (Observer observer : observers.values()) {
          observer.media.updateStatus(status, totalCount, unwatchedCount);
          observer.observer.statusChanged(media, status);
        }
      }
    }

    void changeStatus(PlexLibraryItem.WatchedState status) {


      final int priorLeafTotal = media.getTotalLeaves();
      final int priorLeafUnwatched = media.getUnwatchedLeaves();
      notifyObservers(status,
       status == PlexLibraryItem.WatchedState.Removed? 0 : priorLeafTotal,
       status == PlexLibraryItem.WatchedState.Unwatched ? priorLeafTotal : 0);

      if (!children.isEmpty()) {
        for (MediaEntry media : children.values())
          media.changeStatus(status);
      }

      if (parent != null && parent.media != null) {
        final int newParentLeafTotal = status == PlexLibraryItem.WatchedState.Removed?
         parent.media.getTotalLeaves() - priorLeafTotal : parent.media.getTotalLeaves();
        final int newParentLeafUnwatched = parent.media.getUnwatchedLeaves() +
         (status == PlexLibraryItem.WatchedState.Unwatched ?
          priorLeafTotal - priorLeafUnwatched :  -priorLeafUnwatched);
        parent.notifyObservers(PlexLibraryItem.WatchedState.Refreshed,
         newParentLeafTotal,
         newParentLeafUnwatched);
      }
    }

  }


  private class ObserversMap extends HashMap<String, MediaEntry> {}

}
