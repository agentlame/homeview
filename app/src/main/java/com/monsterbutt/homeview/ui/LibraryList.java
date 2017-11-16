package com.monsterbutt.homeview.ui;


import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.text.TextUtils;

import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.grid.QuickJumpRow;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpDisplay;
import com.monsterbutt.homeview.ui.interfaces.ICardObjectListCallback;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.IContainer;
import us.nineworlds.plex.rest.model.impl.Video;

public class LibraryList {

  private static CardObjectList buildUpdateItem(ICardObjectListCallback callback, IContainer container) {
    List<Directory> directories = container != null ? container.getDirectories() : null;
    List<Video> videos = container != null ? container.getVideos() : null;
    CardObjectList objects = new CardObjectList();
    Iterator<Directory> itDirs = directories != null ? directories.iterator() : null;
    Directory currDir = null;
    if (itDirs != null)
      currDir = itDirs.next();

    Iterator<Video> itVideos = videos != null ? videos.iterator() : null;
    Video currVid = null;
    if (itVideos != null)
      currVid = itVideos.next();

    while (currDir != null || currVid != null) {
      boolean useDir = false;
      if (currDir != null) {
        if (currVid != null) {
          if (currDir.getUpdatedAt() > currVid.getTimeAdded())
            useDir = true;
        } else
          useDir = true;
      }

      PlexLibraryItem item;
      if (useDir) {
        item = PlexContainerItem.getItem(currDir);
        currDir = itDirs.hasNext() ? itDirs.next() : null;
      }
      else {
        item = PlexVideoItem.getItem(currVid);
        currVid = itVideos.hasNext() ? itVideos.next() : null;
      }
      callback.shouldAdd(container, item, objects);
    }
    return objects;
  }

  public static void update(ArrayObjectAdapter adapter, StatusWatcher.StatusWatcherObserver statusWatcher,
                            ICardObjectListCallback callback, IContainer container,
                            IQuickJumpDisplay quickJumpDisplay) {
    CardObjectList list = buildUpdateItem(callback, container);
    Collection<IRegisteredMedia> toRemove = new ArrayList<>();
    for (int currIndex = adapter.size(); 0 != currIndex--; /**/) {
      CardObject item = (CardObject) adapter.get(currIndex);
      if (!list.contains(item.getKey())) {
        adapter.removeItems(currIndex, 1);
        toRemove.add(item);
      }
    }
    statusWatcher.release(toRemove);
    fill(adapter, statusWatcher, list.getList(), quickJumpDisplay);
  }

  public static void fill(ArrayObjectAdapter adapter,
                          StatusWatcher.StatusWatcherObserver statusWatcher, List<CardObject> list,
                          IQuickJumpDisplay quickJumpDisplay) {
    Collection<IRegisteredMedia> toAdd = new ArrayList<>();

    Map<String, QuickJumpRow> map = new HashMap<>();
    for (int newIndex = 0; newIndex < list.size(); ++newIndex) {
      CardObject item = list.get(newIndex);
      String jumpKey = item.getSortTitle().substring(0, 1).toUpperCase();
      if (TextUtils.isDigitsOnly(jumpKey))
        jumpKey = "#";
      QuickJumpRow jump = map.get(jumpKey);
      if (jump == null)
        map.put(jumpKey, new QuickJumpRow(jumpKey, newIndex));
      int currIndex = adapter.indexOf(item);
      if (currIndex >= 0) {
        if (((CardObject) adapter.get(currIndex)).setWatchState(item.getWatchedState()))
          adapter.notifyItemRangeChanged(currIndex, 1);
      }
      else {
        adapter.add(newIndex, item);
        toAdd.add(item);
      }
    }
    statusWatcher.attach(toAdd);

    if (quickJumpDisplay != null) {
      List<QuickJumpRow> items = new ArrayList<>(map.values());
      Collections.sort(items, new Comparator<QuickJumpRow>() {
        @Override
        public int compare(QuickJumpRow lhs, QuickJumpRow rhs) {
          if (lhs.index > rhs.index)
            return 1;
          return -1;
        }
      });
      quickJumpDisplay.setQuickJumpList(items);
    }
  }
}
